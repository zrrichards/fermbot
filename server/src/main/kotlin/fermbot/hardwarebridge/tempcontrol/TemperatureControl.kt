package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
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
class TemperatureActuator @Inject constructor(@param:Named("heater") private val heater: ActiveHighDigitalOutputDevice, @param:Named("cooler") private val cooler: ActiveHighDigitalOutputDevice) {
    var currentMode = HeatingMode.OFF
        private set

    /**
     * Sets the current heating mode
     * Returns the previous heating mode
     */
    fun setHeatingMode(heatingMode: HeatingMode) : HeatingMode {
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

@Factory
class HardwareBackedHeaterCoolerFactory @Inject constructor(private val gpioManager: GpioManager) : HeaterCoolerFactory {

    @Value("\${fermbot.heater-pin-name}")
    private var heaterPinName: String? = null

    @Value("\${fermbot.cooler-pin-name}")
    private var coolerPinName: String? = null

    @Bean
    @Singleton
    @Named("heater")
    override fun createHeater(): ActiveHighDigitalOutputDevice {
        validate()
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(heaterPinName!!, "Heater"))
    }

    @Bean
    @Singleton
    @Named("cooler")
    override fun createCooler(): ActiveHighDigitalOutputDevice {
        validate()
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(coolerPinName!!, "Cooler"))
    }


    //todo this should be in an init block for this class. messing around with micronaut so had to move it
    private fun validate() {
        require(heaterPinName != null) {
            """Heater pin must be defined. Set the following property in application.yml:
                |fermbot:
                |    heater-pin-name:
            """.trimMargin()
        }

        require(coolerPinName != null) {
            """Cooler pin must be defined. Set the following property in application.yml:
                |fermbot:
                |    cooler-pin-name:
            """.trimMargin()
        }

        require(heaterPinName != coolerPinName) {
            "Both the heater and cooler are defined on pin $heaterPinName. They must be defined on different pins"
        }
    }
}
