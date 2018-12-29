package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/20/19
 */
interface ActiveHighDigitalOutputDevice {

    fun isEnabled(): Boolean
    fun enable()
    fun disable()
}

class HardwareBackedActiveHighDigitalOutputDevice(private val outputPin: DigitalOutput) : ActiveHighDigitalOutputDevice {
    override fun isEnabled(): Boolean {
        return outputPin.isHigh()
    }

    override fun enable() {
        outputPin.setHigh()
    }

    override fun disable() {
        outputPin.setLow()
    }
}

//FIXME shut down gpio if micronaut shuts down

/**
 * This class is responsible for actuating the heating and cooling devices. It knows nothing about temperature setpoints.
 * Its primary role is to ensure that the heater and cooler are both not enabled at the same time.
 */
@Singleton
class HardwareBackedTemperatureActuator @Inject constructor(@param:Named("heater") private val heater: ActiveHighDigitalOutputDevice, @param:Named("cooler") private val cooler: ActiveHighDigitalOutputDevice) : TemperatureActuator{

    private var currentMode = HeatingMode.OFF

    private val logger = LoggerFactory.getLogger(HardwareBackedTemperatureActuator::class.java)

    override fun getCurrentHeatingMode() = currentMode

    /**
     * Sets the current heating mode
     * Returns the previous heating mode
     */
    override fun setHeatingMode(heatingMode: HeatingMode) : HeatingMode {
        logger.debug("Setting heating mode to: {}", heatingMode)
        when (heatingMode) {
            HeatingMode.OFF -> {
                heater.disable()
                cooler.disable()
            }
            HeatingMode.HEATING -> {
                heater.enable()
                cooler.disable()
            }
            HeatingMode.COOLING -> {
                heater.disable()
                cooler.enable()
            }
        }

        //this can only happen due to a programming error but check anyway
        if (heater.isEnabled() && cooler.isEnabled()) {
            heater.disable()
            cooler.disable()
            throw IllegalStateException("Both Heater and cooler enabled simultaneously. Disabling both. This is a programming error. Please report this issue on github immediately")
        }

        val prevHeatingMode = currentMode
        currentMode = heatingMode
        return prevHeatingMode
    }
}

/**
 * The way to control heating and cooling for the FermBot. This is the **ONLY** way you should attempt
 * to enable or disable heating and cooling. Since the heater and cooler are their own digital devices,
 * nothing inherently prevents both from being enabled at the same time. This actuator ensure that
 * cannot happen. Do not manipulate the heater and cooler directly under any circumstances.
 */
interface TemperatureActuator {
    fun setHeatingMode(heatingMode: HeatingMode): HeatingMode
    fun getCurrentHeatingMode(): HeatingMode
}

@Factory
class HardwareBackedHeaterCoolerFactory @Inject constructor(private val gpioManager: GpioManager,
            @Property(name="fermbot.heater-pin-name") private val heaterPinName: String,
            @Property(name="fermbot.cooler-pin-name") private val coolerPinName: String) : HeaterCoolerFactory {

    private val logger = LoggerFactory.getLogger(HardwareBackedHeaterCoolerFactory::class.java)

    init {
        require(heaterPinName != coolerPinName) {
            "Both the heater and cooler are defined on pin $heaterPinName. They must be defined on different pins"
        }
    }

    @Bean
    @Singleton
    @Named("heater")
    override fun createHeater(): ActiveHighDigitalOutputDevice {
        logger.debug("Registering heater on pin {}", heaterPinName)
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(heaterPinName!!, "Heater"))
    }

    @Bean
    @Singleton
    @Named("cooler")
    override fun createCooler(): ActiveHighDigitalOutputDevice {
        logger.debug("Registering cooler on pin {}", coolerPinName)
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(coolerPinName!!, "Cooler"))
    }
}
