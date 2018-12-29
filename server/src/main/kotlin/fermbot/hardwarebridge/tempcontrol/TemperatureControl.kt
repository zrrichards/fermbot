package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.*
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.*
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

    fun isDisabled() : Boolean {
        return !isEnabled()
    }

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

/**
 * This class is responsible for actuating the heating and cooling devices. It knows nothing about temperature setpoints.
 * Its primary role is to ensure that the heater and cooler are both not enabled at the same time.
 */
@Singleton
class HardwareBackedTemperatureActuator @Inject constructor(@param:Named("heater") private val heater: Optional<ActiveHighDigitalOutputDevice>, @param:Named("cooler") private val cooler: Optional<ActiveHighDigitalOutputDevice>) : TemperatureActuator{

    private var currentMode = HeatingMode.OFF

    private val logger = LoggerFactory.getLogger(HardwareBackedTemperatureActuator::class.java)

    val heatingCoolingConfiguration = determineHeaterCoolerConfiguration()

    override fun getCurrentHeatingMode() = currentMode

    init {
        logger.info("Initializing Temperature Actuator. ${heater.ifPresent { "Heater is enabled " }} ${cooler.ifPresent { "Cooler is enabled" }}. Heating mode is currently $currentMode")
        if (heatingCoolingConfiguration == HeaterCoolerConfiguration.NONE) {
            logger.warn("No temperature control devices are enabled. The FermBot will not be able to control your fermentation temperature. It will only be monitored. Ensure this is what you want before proceeding")
        }
    }

    /**
     * Sets the current heating mode
     * Returns the previous heating mode
     */
    override fun setHeatingMode(heatingMode: HeatingMode) : HeatingMode {
        logger.debug("Setting heating mode to: {}", heatingMode)

        when (heatingCoolingConfiguration) {
            HeaterCoolerConfiguration.NONE -> {
               logger.warn("No temperature control devices enabled. Ignoring request to set heating mode to $heatingMode")
            }
            HeaterCoolerConfiguration.HEATER -> {
                if (heatingMode == HeatingMode.COOLING ) {
                    logger.warn("No cooling device enabled. Ignoring request to set heating mode to $heatingMode")
                }
            }
            HeaterCoolerConfiguration.COOLER -> {
                if (heatingMode == HeatingMode.HEATING) {
                    logger.warn("No heating device enabled. Ignoring request to set heating mode to $heatingMode")
                }
            }
            HeaterCoolerConfiguration.BOTH -> { /* do nothing. both devices are configured */ }
        }


        /* Ensure disable is called first so that there is no time when both are enabled simultaneously
         * Also, put a small pause to ensure that the pin has time to set to low before enabling the other device
         */
        when (heatingMode) {
            HeatingMode.OFF -> {
                heater.ifPresent { it.disable() }
                cooler.ifPresent { it.disable() }
            }
            HeatingMode.HEATING -> {
                cooler.ifPresent {
                    it.disable()
                    sleep(100)
                }
                heater.ifPresent { it.enable() }
            }
            HeatingMode.COOLING -> {
                heater.ifPresent {
                    it.disable()
                    sleep(100)
                }
                cooler.ifPresent { it.enable() }
            }
        }

        //this can only happen due to a programming error but check anyway
        if (heater.isPresent && heater.get().isEnabled() && cooler.isPresent && cooler.get().isEnabled()) {
            heater.get().disable()
            cooler.get().disable()
            throw IllegalStateException("Both Heater and cooler enabled simultaneously. Disabling both. This is a programming error. Please report this issue on github immediately")
        }

        val prevHeatingMode = currentMode
        currentMode = heatingMode
        return prevHeatingMode
    }

    private fun determineHeaterCoolerConfiguration(): HeaterCoolerConfiguration {
        return if (heater.isPresent) {
            if (cooler.isPresent) {
                HeaterCoolerConfiguration.BOTH
            } else {
                HeaterCoolerConfiguration.HEATER
            }
        } else {
            if (cooler.isPresent) {
                HeaterCoolerConfiguration.COOLER
            } else {
                HeaterCoolerConfiguration.NONE
            }

        }
    }
}

enum class HeaterCoolerConfiguration {
    NONE,
    HEATER,
    COOLER,
    BOTH
}

/**
 * The way to control heating and cooling for the FermBot. This is the **ONLY** way you should attempt
 * to enable or disable heating and cooling. Since the heater and cooler are their own digital devices,
 * nothing inherently prevents both from being enabled at the same time. This actuator ensures that
 * cannot happen. Do not manipulate the heater and cooler directly under any circumstances.
 */
interface TemperatureActuator {
    fun setHeatingMode(heatingMode: HeatingMode): HeatingMode
    fun getCurrentHeatingMode(): HeatingMode
}

@Factory
class HardwareBackedHeaterCoolerFactory @Inject constructor(private val gpioManager: GpioManager,
            @Property(name="fermbot.heater.pin-name") private val heaterPinName: String,
            @Property(name="fermbot.cooler.pin-name") private val coolerPinName: String) : HeaterCoolerFactory {

    private val logger = LoggerFactory.getLogger(HardwareBackedHeaterCoolerFactory::class.java)

    init {
        require(heaterPinName != coolerPinName) {
            "Both the heater and cooler are defined on pin $heaterPinName. They must be defined on different pins"
        }
    }

    @Bean
    @Singleton
    @Named("heater")
    @Requires(property="fermbot.heater.enabled", value="true")
    override fun createHeater(): ActiveHighDigitalOutputDevice {
        logger.debug("Registering heater on pin {}", heaterPinName)
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(heaterPinName!!, "Heater"))
    }

    @Bean
    @Singleton
    @Named("cooler")
    @Requires(property="fermbot.cooler.enabled", value="true")
    override fun createCooler(): ActiveHighDigitalOutputDevice {
        logger.debug("Registering cooler on pin {}", coolerPinName)
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(coolerPinName!!, "Cooler"))
    }
}
