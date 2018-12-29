package fermbot.hardwarebridge

import fermbot.Thermometer
import io.micronaut.context.annotation.Requires
import java.util.*
import javax.inject.Singleton

@Singleton
@Requires(property="fermbot.ds18b20.enabled", notEquals="true")
class NullThermometerReader : ThermometerReader {
    override fun getDevices(): Optional<Thermometer> {
        return Optional.empty()
    }
}

