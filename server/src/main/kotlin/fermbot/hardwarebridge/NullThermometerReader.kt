package fermbot.hardwarebridge

import fermbot.Thermometer
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Requires(property= FermbotProperties.isDs18b20Enabled, notEquals="true")
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

