package fermbot.hardwarebridge

import fermbot.Temperature
import fermbot.Thermometer
import io.micronaut.context.annotation.Requires
import java.time.Instant
import javax.inject.Singleton

interface DS18B20Manager : ThermometerReader {

    override fun getDevices(): DS18B20
}

@Singleton
@Requires(property="ds18b20-enabled", notEquals="true")
object NullThermometerReader : ThermometerReader {
    override fun getDevices(): Thermometer {
        return NullThermometer
    }
}

object NullThermometer : Thermometer {
    override val currentTemp: Temperature
        get() = throw UnsupportedOperationException("Cannot get currentTemp for null thermometer")

    override val timestamp: Instant
        get() = throw UnsupportedOperationException("Cannot get timestamp for null thermometer")

    override val id = "Null-Thermometer"
}
