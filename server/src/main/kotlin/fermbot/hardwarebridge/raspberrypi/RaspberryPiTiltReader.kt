package fermbot.hardwarebridge.raspberrypi
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

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.hardwarebridge.ThermoHydrometer
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.Tilt
import fermbot.hardwarebridge.TiltColors
import fermbot.profile.Environments
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.io.readText

/**
 * This class is the bride to the python-based tilt code on the raspberry pi
 * @version $ 12/5/19
 */
@Singleton
@Requires(env=[Environments.RASPBERRY_PI], property= FermbotProperties.isTiltEnabled, value="true")
class RaspberryPiTiltReader : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(RaspberryPiTiltReader::class.java)

    init {
        logger.info("Tilt is enabled. Loading Raspberry Pi reader")
    }

    @Value("\${fermbot.pytilt-script-path}")
    private lateinit var pathToPytiltScript: String

    override fun readTilt(): Optional<ThermoHydrometer> {
        val pytiltScript = "$pathToPytiltScript/pytilt.py"
        val process = ProcessBuilder("python", pytiltScript).start()
        val exited = process.waitFor(10, TimeUnit.SECONDS)
        if (!exited || process.exitValue() != 0) {
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val fileExists = File(pytiltScript).exists()
            throw RuntimeException("Unable to read Tilt: $error, Current working directory: ${getWorkingDirectory()}. Attempted path to pytilt script: $pathToPytiltScript. File exists? $fileExists")
        }
        val objectMapper = ObjectMapper()
        val tilt = objectMapper.readValue(process.inputStream, Tilt::class.java)
        logger.debug("Read Tilt: {}", tilt)
        return Optional.of(tilt)
    }

    private fun getWorkingDirectory(): String {
        return Paths.get("").toAbsolutePath().toString()
    }
}

@Singleton
@Primary
@Requires(property=FermbotProperties.isTiltEnabled, notEquals="true")
class NullTiltReader : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(NullTiltReader::class.java)

    init {
        logger.info("Loading NullTiltReader because Tilt is disabled")
    }

    override fun readTilt(): Optional<ThermoHydrometer> {
        logger.debug("Read Tilt: [NullTilt]")
        return Optional.empty()
    }
}

@Singleton
@Controller
@Requires(env=[Environments.SIMULATION], property=FermbotProperties.isTiltEnabled, value="true")
class SimulationTiltReader : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(SimulationTiltReader::class.java)

    private val og = 1.070
    private val fg = 1.020
    private var decayFactor = 0.01
    private var decayAmount = 1 - decayFactor

    init {
        logger.info("Initializing SimulationTiltReader")
    }

    private var tilt: Optional<ThermoHydrometer> = createTilt(og)

    override fun readTilt(): Optional<ThermoHydrometer> {
        if (tilt.get().timestamp < Instant.now().minusMillis(6)) {
            refresh()
        }
        return tilt
    }

    private fun refresh() {
        val nextValue = (og - fg) * decayAmount + fg
        decayAmount *= decayAmount
        tilt = createTilt(nextValue)
    }

    private fun createTilt(nextValue: Double): Optional<ThermoHydrometer> = Optional.of(Tilt(color = TiltColors.BLACK, specificGravity = nextValue, rawTemp = 45.2))
}