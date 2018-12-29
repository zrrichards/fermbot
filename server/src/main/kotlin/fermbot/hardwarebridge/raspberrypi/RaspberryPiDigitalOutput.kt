package fermbot.hardwarebridge.raspberrypi

import com.pi4j.io.gpio.GpioPinDigitalOutput
import fermbot.hardwarebridge.DigitalOutput

/**
 * Thin wrapper around raspberry pi specific GPIo implementation hide pi4j from the rest of the fermbot
 */
class RaspberryPiDigitalOutput(private val outputPin: GpioPinDigitalOutput) : DigitalOutput {
    override fun setLow() {
        outputPin.low()
    }

    override fun isHigh(): Boolean {
       return outputPin.isHigh
    }

    override fun setHigh() {
        return outputPin.high()
    }
}

