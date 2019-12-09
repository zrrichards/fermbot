package fermbot

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
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
        val process = ProcessBuilder("python", "$pathToPytiltScript/pytilt.py").start()
        val exited = process.waitFor(10, TimeUnit.SECONDS)
        if (!exited || process.exitValue() != 0) {
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            throw RuntimeException("Unable to read Tilt: $error, Current working directory: ${getWorkingDirectory()}")
        }
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(process.inputStream, Tilt::class.java)
    }

    private fun getWorkingDirectory(): String {
        return Paths.get("").toAbsolutePath().toString()
    }
}

