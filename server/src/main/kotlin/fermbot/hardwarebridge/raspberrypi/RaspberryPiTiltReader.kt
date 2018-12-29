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
import fermbot.Configuration
import fermbot.hardwarebridge.*
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.readText

/**
 * This class is the bride to the python-based tilt code on the raspberry pi
 * @version $ 12/5/19
 */
@Singleton
@Requires(env=["Raspberry-Pi"], property="tilt-enabled", value="true")
class RaspberryPiTiltReader @Inject constructor(configuration: Configuration) : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(RaspberryPiTiltReader::class.java)

    init {
        logger.debug("Tilt is enabled. Loading reader")
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
@Requires(property="tilt-enabled", notEquals="true")
class RaspberryPiNullTiltReader : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(NullThermometerReader::class.java)

    init {
        logger.debug("Tilt is disabled")
    }

    override fun readTilt(): Optional<ThermoHydrometer> {
        logger.debug("Read Tilt: [NullTilt]")
        return Optional.empty()
    }
}

