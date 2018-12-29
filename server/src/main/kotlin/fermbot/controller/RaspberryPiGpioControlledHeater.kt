package fermbot.controller

import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import fermbot.Configuration
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
class RaspberryPiGpioControlledHeater @Inject constructor(configuration: Configuration) : Heater {

    private val pin: GpioPinDigitalOutput
    private val pinName = RaspiPin.getPinByName(configuration.heaterPinName)

    private val logger = LoggerFactory.getLogger(RaspberryPiGpioControlledHeater::class.java)

    init {
        logger.info("Provisioning heater control on pin: ${pinName.name}")
        pin = GpioFactory.getInstance().provisionDigitalOutputPin(pinName, "HeaterRelayControl", PinState.LOW)
    }

    override fun enable() {
        logger.debug("Enabling heater on pin {}", pinName)
        pin.high()
    }

    override fun disable() {
        logger.debug("Disabling heater on pin {}", pinName)
        pin.low()
    }

    override fun isEnabled(): Boolean {
        return pin.isHigh
    }
}