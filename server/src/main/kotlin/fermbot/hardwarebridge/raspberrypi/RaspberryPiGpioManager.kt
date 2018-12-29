package fermbot.hardwarebridge.raspberrypi

import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/12/19
 */
@Singleton
class RaspberryPiGpioManager : GpioManager {
    override fun provisionDigitalOutputDevice(pinName: String, name: String): DigitalOutput {
        val pin = RaspiPin.getPinByName(pinName)
        return RaspberryPiDigitalOutput(GpioFactory.getInstance().provisionDigitalOutputPin(pin, name, PinState.LOW))
    }
}