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

    private val gpioDevices = mutableMapOf<Pin, GpioPinDigitalOutput>()

    init {
        logger.debug("Initializing Raspberry Pi GPIO Manager")
    }

    @PreDestroy
    override fun shutdown() {
        isShutDown = true
        logger.info("Shutting down GPIO manager")
        gpioDevices.forEach {
            logger.debug("Shutting down device on pin: {}", it.key.name)
            it.value.low()
            GpioFactory.getInstance().unprovisionPin(it.value)
        }
        gpioDevices.clear()
        GpioFactory.getInstance().shutdown()
    }

    override fun provisionDigitalOutputDevice(pinName: String, name: String): DigitalOutput {
        check (!isShutDown)
        val pin = RaspiPin.getPinByName(pinName)

        check (!gpioDevices.containsKey(pin)) {
            "Device already provisioned on pin $pinName. Please select a different pin"
        }

        val outputPin = GpioFactory.getInstance().provisionDigitalOutputPin(pin, name, PinState.LOW)
        gpioDevices[pin] = outputPin
        logger.debug("Provisioning device on pin: {}. Total devices provisioned: {}", pinName, gpioDevices.size)
        return RaspberryPiDigitalOutput(outputPin)
    }
}