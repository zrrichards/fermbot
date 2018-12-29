package fermbot
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
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.readText

/**
 *
 * @version $ 12/5/19
 */
@Singleton
class TiltReader @Inject constructor(configuration: Configuration) {

    private val pathToPytiltScript = configuration.pytiltScriptPath

    fun readTilt(): Tilt {
        val pytiltScript = "$pathToPytiltScript/pytilt.py"
        val process = ProcessBuilder("python", pytiltScript).start()
        val exited = process.waitFor(10, TimeUnit.SECONDS)
        if (!exited || process.exitValue() != 0) {
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val fileExists = File(pytiltScript).exists()
            throw RuntimeException("Unable to read Tilt: $error, Current working directory: ${getWorkingDirectory()}. Attempted path to pytilt script: $pathToPytiltScript. File exists? $fileExists")
        }
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(process.inputStream, Tilt::class.java)
    }

    private fun getWorkingDirectory(): String {
        return Paths.get("").toAbsolutePath().toString()
    }
}

