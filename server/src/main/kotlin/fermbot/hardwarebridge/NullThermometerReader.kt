package fermbot.hardwarebridge

import fermbot.Thermometer
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Requires(property="fermbot.ds18b20.enabled", notEquals="true")
class NullThermometerReader : ThermometerReader {

    private val logger = LoggerFactory.getLogger(NullThermometerReader::class.java)

    init {
        "Initializing: ${this.javaClass.simpleName}"
    }

    override fun getDevices(): Optional<Thermometer> {
        logger.debug("Read Null Thermometer (Optional.empty)")
        return Optional.empty()
    }
}

