package fermbot.hardwarebridge.raspberrypi

import com.pi4j.io.gpio.*
import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.profile.Environments
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import javax.annotation.PreDestroy
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/12/19
 */
@Singleton
@Requires(env=[Environments.RASPBERRY_PI])
class RaspberryPiGpioManager : GpioManager {

    private val logger = LoggerFactory.getLogger(RaspberryPiGpioManager::class.java)

    private var isShutDown = false

    private val gpioDevices = mutableMapOf<Pin, GpioPinDigitalOutputDevice>()

    init {
        logger.debug("Initializing Raspberry Pi GPIO Manager")
    }

    @PreDestroy
    override fun shutdown() {
        isShutDown = true
        logger.info("Shutting down GPIO manager")
        gpioDevices.forEach {
            logger.info("Shutting down device on pin: ${it.key.name}. Setting state to ${it.value.inactiveState}")
            when (it.value.inactiveState) {
                PinState.LOW -> it.value.pin.low()
                PinState.HIGH -> it.value.pin.high()
            }
            GpioFactory.getInstance().unprovisionPin(it.value.pin)
        }
        gpioDevices.clear()
        GpioFactory.getInstance().shutdown()
    }

    override fun provisionActiveLowDigitalOutput(pinName: String, name: String): DigitalOutput {
        check (!isShutDown)
        val pin = RaspiPin.getPinByName(pinName) ?: throw IllegalArgumentException("Unrecognized Pin named $pinName. Please see the README for valid values")

        check (!gpioDevices.containsKey(pin)) {
            "Device already provisioned on pin $pinName. Please select a different pin"
        }

        val outputPin = GpioFactory.getInstance().provisionDigitalOutputPin(pin, name, PinState.HIGH)
        gpioDevices[pin] = GpioPinDigitalOutputDevice(outputPin, PinState.HIGH)
        logger.info("""Provisioning ACTIVE LOW device named "$name" on Pin "$pinName". Total number of devices provisioned ${gpioDevices.size}""")
        return RaspberryPiDigitalOutput(outputPin)
    }

}

data class GpioPinDigitalOutputDevice(val pin: GpioPinDigitalOutput, val inactiveState: PinState)