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
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.profile.FermentationProfileController
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
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
class FermentationMonitorTask @Inject constructor(private val brewfather: Optional<Brewfather>, private val thermoHydrometerReader: ThermoHydrometerReader, private val thermometerReader: ThermometerReader, private val persister: FermentationSnapshotPersister) : Runnable {

    val allSnapshots: List<String>
        get() = persister.readAsCsv()

    private val logger = LoggerFactory.getLogger(FermentationMonitorTask::class.java)

    private val queue = FermentationSnapshotQueue(persister, Duration.ofHours(7))

    var fermentationProfileController: FermentationProfileController? = null

    init {
        if (brewfather.isPresent) {
            logger.info("Brewfather is configured.")
        } else {
            logger.warn("Brewfather is not configured. Fermentation monitoring will only consist of logging to the console")
        }
    }

    override fun run() {

        val tiltOptional = thermoHydrometerReader.readTilt()
        val thermometerOptional = thermometerReader.getDevices()

        val output = StringBuilder()
        var currentTemp : Temperature? = null
        var currentSg : Double? = null

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
            output.append("Tilt reading ${tiltHigh.toStringF()} higher than thermometer")
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

        //if brewfather is enabled and we have one of either the temp or SG to log
        if (brewfather.isPresent) {
            if(currentTemp != null || currentSg != null) {
                val tempOptional = Optional.ofNullable(currentTemp)
                val sgOptional = Optional.ofNullable(currentSg)

                val commentString = """Current Setpoint: ${fermentationProfileController?.currentSetpoint?.temperature}\nCurrent Heating Mode: ${fermentationProfileController?.getCurrentHeatingMode()}"""

                val result = brewfather.get().updateBatchDetails(tempOptional, sgOptional, commentString)
                if (result.isSuccessful()) {
                    logger.info("Successfully logged to brewfather.")
                } else {
                    logger.warn("Unable to log to brewfather. reason: ${result.result}")
                }
            } else {
                logger.warn("Brewfather configured but current temp and specific gravity cannot be read. Nothing to upload.")
            }
        } else {
            logger.debug("Brewfather not enabled, nothing to upload")
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

