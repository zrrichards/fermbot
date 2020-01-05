package fermbot.hardwarebridge

import fermbot.hardwarebridge.ThermoHydrometer
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Primary
@Requires(property= FermbotProperties.isTiltEnabled, notEquals="true")
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