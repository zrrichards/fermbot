package fermbot.monitor
/*  Fermbot - Open source fermentation monitoring software.
 *  Copyright (C) 2019 Zachary Richards
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import fermbot.Temperature
import fermbot.brewfather.Brewfather
import fermbot.cascadeOptionals
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.orchestrator.SystemStatistics
import fermbot.profile.FermentationProfileController
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * Controls tilt parsing and logs data to brewfather
 * @author Zachary Richards
 * @version $ 12/5/19
 */
@Singleton
class FermentationMonitorTask @Inject constructor(private val thermoHydrometerReader: ThermoHydrometerReader, private val thermometerReader: ThermometerReader, private val persister: FermentationSnapshotPersister) : Runnable {

    val snapshotsAsCsv: List<String>
        get() = persister.readAsCsv()

    private val logger = LoggerFactory.getLogger(FermentationMonitorTask::class.java)

    private val queue = FermentationSnapshotQueue(persister, Duration.ofHours(7))

    val mostRecentSnapshot: FermentationSnapshot
        get() = queue.newest

    var fermentationProfileController: FermentationProfileController? = null


    override fun run() {
        try {
            doRun()
        } catch (e: Exception) {
            logger.error("Caught error trying to run fermentation monitoring. Ignoring", e)
        }
    }

    private fun doRun() {

        val tiltOptional = thermoHydrometerReader.readTilt()
        val thermometerOptional = thermometerReader.getDevices()

        val output = StringBuilder()
        var currentTemp: Temperature? = null
        var currentSg: Double? = null

        tiltOptional.ifPresent {
            val toStringF = it.currentTemp.toStringF()
            output.append("Tilt[$toStringF, sg=${it.specificGravity}] ")
            currentTemp = it.currentTemp
            currentSg = it.specificGravity
        }

        thermometerOptional.ifPresent {
            val toStringF = it.currentTemp.toStringF()
            output.append("Thermometer[$toStringF] ")
            currentTemp = it.currentTemp
        }

        if (tiltOptional.isPresent && thermometerOptional.isPresent) {
            val tiltHigh = (tiltOptional.get().currentTemp - thermometerOptional.get().currentTemp).round(1)
            output.append("Tilt reading ${tiltHigh.toStringF()} higher than thermometer ")
        }

        if (fermentationProfileController != null) {
            output.append("Heating Mode[${fermentationProfileController?.getCurrentHeatingMode()}]")
        }

        if (output.isNotEmpty()) {
            logger.info(output.toString())
        }

        if (fermentationProfileController != null) {
            queue.add(FermentationSnapshot(
                    currentSg = currentSg,
                    temp = currentTemp,
                    currentSetpointIndex = fermentationProfileController!!.currentSetpointIndex,
                    heatingMode = fermentationProfileController!!.getCurrentHeatingMode(),
                    setpoint = fermentationProfileController!!.currentSetpoint.temperature,
                    description = fermentationProfileController!!.currentSetpoint.description
            ))
        }
    }

    fun clearSnapshots() {
        queue.clear()
        persister.clear()
    }

    fun averageGravityFromPast(within: Duration): Double {
        val timestamp = Instant.now() - within
        val range = if (queue.oldest.timestamp > timestamp) {
            logger.warn("Oldest timestamp in memorty is ${queue.oldest.timestamp} which is more recent than desired timestamp of $timestamp. Ignoring older values")
            queue.oldest.timestamp
        } else {
            timestamp
        }

        return queue.getAll().asReversed().takeWhile { it.timestamp >= range }.mapNotNull { it.currentSg }.average()
    }
}

private fun Temperature.round(decimals: Int): Temperature {

    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    return Temperature(this.value.round(decimals), this.unit)
}


@Singleton
class BrewfatherUploadTask @Inject constructor(private val brewfather: Optional<Brewfather>, private val thermoHydrometerReader: ThermoHydrometerReader, private val thermometerReader: ThermometerReader) : Runnable {

    private val logger = LoggerFactory.getLogger(BrewfatherUploadTask::class.java)
    var fermentationProfileController: FermentationProfileController? = null

    init {
        if (brewfather.isPresent) {
            logger.info("Brewfather is configured.")
        } else {
            logger.warn("Brewfather is not configured. Fermentation monitoring will only consist of logging to the console")
        }
    }

    override fun run() {
        try {
            run0()
        } catch (e: Exception) {
            logger.warn("Caught error trying to upload to Brewfather. Ignoring", e)
        }
    }

    @Inject private lateinit var systemStatistics: SystemStatistics

    private var lastSuccessfulUpload: Instant //a null safe view of the value in system statistics
        get() = systemStatistics.lastSuccessfulUpload ?: Instant.now().minus(1, ChronoUnit.DAYS)
        set(value) { systemStatistics.lastSuccessfulUpload = value }

    private fun hasEnoughTimePassedToUploadAgain() = Duration.between(lastSuccessfulUpload, Instant.now()) > Duration.ofMinutes(15).plusSeconds(10)

    private fun run0() {



        if (!hasEnoughTimePassedToUploadAgain()) {
            return
        }

        val tilt = thermoHydrometerReader.readTilt()
        val currentSg: Double? = tilt.map { it.specificGravity }.orElse(null)
        val currentTemp: Temperature? = cascadeOptionals(tilt, thermometerReader.getDevices()).map { it.currentTemp }.orElse(null)

        //if brewfather is enabled and we have one of either the temp or SG to log
        if (brewfather.isPresent) {
            if (currentTemp != null || currentSg != null) {
                val tempOptional = Optional.ofNullable(currentTemp)
                val sgOptional = Optional.ofNullable(currentSg)

                val commentString = """Current Setpoint: ${fermentationProfileController?.currentSetpoint?.temperature}\nCurrent Heating Mode: ${fermentationProfileController?.getCurrentHeatingMode()}"""

                val result = brewfather.get().updateBatchDetails(tempOptional, sgOptional, commentString)
                when {
                    result.isSuccessful() -> logger.info("Successfully logged to brewfather.")
                    result.isTooEarly() -> { /* do nothing. This is expected */ }
                    else -> logger.warn("Unable to log to brewfather. reason: ${result.result}")
                }
            } else {
                logger.warn("Brewfather configured but current temp and specific gravity cannot be read. Nothing to upload.")
            }
        } else {
            logger.debug("Brewfather not enabled, nothing to upload")

        }
    }
}