package fermbot.ds18b20

import com.pi4j.component.temperature.TemperatureSensor
import com.pi4j.component.temperature.impl.TmpDS18B20DeviceType
import com.pi4j.io.w1.W1Device
import com.pi4j.io.w1.W1Master
import com.pi4j.temperature.TemperatureScale
import fermbot.toC
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.inject.Singleton

/**
 * Manages all 1-wire DS18B20 devices connected to the Pi
 * @author Zachary Richards
 * @version 12/11/19
 */
@Singleton
class DS18B20Manager {
    private val w1master = W1Master()

    private val corrector = DefaultDS18B20TemperatureCorrector

    private val logger = LoggerFactory.getLogger(DS18B20Manager::class.java)

    init {
        if (logger.isDebugEnabled) {
            logger.debug("W1Devices present:\n$w1master")
        }
    }

    /**
     * Read DS18B20 device and return it. In order to 'reread' a thermometer, this method must be called multiple times
     */
    fun getDevices(): DS18B20 {
        val w1Devices = w1master.getDevices<W1Device>(TmpDS18B20DeviceType.FAMILY_CODE)
        check(w1Devices.size == 1)  { "Only a single DS18B20 is supported right now. Number found: ${w1Devices.size}. Please report this issue on GitHub"}
        val device = w1Devices[0]
        device as TemperatureSensor
        val temp = corrector(device.getTemperature(TemperatureScale.CELSIUS).toC())
        val ds18b20 = DS18B20(device.id, temp, Instant.now())
        logger.debug("Read DS18B20: {}", ds18b20)
        return ds18b20
    }
}