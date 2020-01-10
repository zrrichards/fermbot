package fermbot.hardwarebridge.raspberrypi

import com.pi4j.component.temperature.TemperatureSensor
import com.pi4j.component.temperature.impl.TmpDS18B20DeviceType
import com.pi4j.io.w1.W1Device
import com.pi4j.io.w1.W1Master
import com.pi4j.temperature.TemperatureScale
import fermbot.Thermometer
import fermbot.hardwarebridge.*
import fermbot.profile.Environments
import fermbot.profile.FermbotProperties
import fermbot.toC
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all 1-wire DS18B20 devices connected to the Pi
 * @author Zachary Richards
 * @version 12/11/19
 */
@Singleton
@Requires(env=[Environments.RASPBERRY_PI], property= FermbotProperties.isDs18b20Enabled, value="true")
class RaspberryPiDS18B20Manager @Inject constructor(private val corrector: TemperatureCorrector) : ThermometerReader {

    private val w1master = W1Master()

    private val logger = LoggerFactory.getLogger(RaspberryPiDS18B20Manager::class.java)

    init {
        "Initializing: ${this.javaClass.simpleName}"
        "Temperature corrector: $corrector"
    }

    private var lastReadDevice: Optional<Thermometer> = Optional.empty()

    /**
     * Read DS18B20 device and return it. In order to 'reread' a thermometer, this method must be called multiple times
     */
    override fun getDevices(): Optional<Thermometer> {
        if (logger.isDebugEnabled) {
            logger.debug("W1Devices present:\n$w1master")
        }

        lastReadDevice = try {
            val w1Devices = w1master.getDevices<W1Device>(TmpDS18B20DeviceType.FAMILY_CODE)
            check(w1Devices.size == 1) { "Only a single DS18B20 is supported right now. Number found: ${w1Devices.size}. Please report this issue on GitHub" }
            val device = w1Devices[0]
            device as TemperatureSensor
            val temp = corrector(device.getTemperature(TemperatureScale.CELSIUS).toC())
            val ds18b20 = DS18B20(device.id, temp, Instant.now())
            logger.debug("Read DS18B20: {}", ds18b20)
            return Optional.of(ds18b20)
        } catch (e: Exception) {
            logger.error("Caught exception when attempting to read DS18B20", e)
            val ageOfReading = if (lastReadDevice.isPresent) {
                Duration.between(lastReadDevice.get().timestamp, Instant.now())
            } else {
                Duration.ofSeconds(Int.MAX_VALUE.toLong())
            }
            if (ageOfReading > Duration.ofMinutes(20)) {
                logger.warn("DS18B20 value not updated in $ageOfReading. Returning no result for safety")
                Optional.empty()
            } else {
                logger.warn("Using previous value of DS18B20: $lastReadDevice")
                lastReadDevice
            }
        }

        return lastReadDevice
    }
}