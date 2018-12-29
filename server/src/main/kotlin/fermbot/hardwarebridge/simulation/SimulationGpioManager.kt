package fermbot.hardwarebridge.simulation

import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.Pin
import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.hardwarebridge.raspberrypi.RaspberryPiGpioManager
import fermbot.profile.Environments
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@Singleton
@Requires(notEnv=[Environments.RASPBERRY_PI])
class SimulationGpioManager : GpioManager {

    private val logger = LoggerFactory.getLogger(RaspberryPiGpioManager::class.java)

    private val gpioDevices = mutableMapOf<String, SimulationDigitalOutputDevice>()

    override fun provisionDigitalOutputDevice(pinName: String, name: String): DigitalOutput {
        check(!gpioDevices.containsKey(pinName))
        val device = SimulationDigitalOutputDevice()
        gpioDevices[pinName] = device
        return device
    }

    override fun shutdown() {
        //do nothing
    }
}

class SimulationDigitalOutputDevice : DigitalOutput {

    var isStateHigh = true

    override fun setHigh() {
        isStateHigh = true
    }

    override fun setLow() {
        isStateHigh = false
    }

    override fun isHigh(): Boolean {
        return isStateHigh
    }

}
