package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import fermbot.profile.BeanDefinitions
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
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

@Factory
class HeatingCoolingConfigurationFactory {

    @Bean
    @Singleton
    fun determineHeatingCoolingConfiguration(@Named(BeanDefinitions.HEATER) heater: Optional<ActiveHighDigitalOutputDevice>, @Named(BeanDefinitions.COOLER) cooler: Optional<ActiveHighDigitalOutputDevice>): HeaterCoolerConfiguration {
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

enum class HeaterCoolerConfiguration(val allowableHeatingModes: List<HeatingMode>) {
    NONE(listOf(HeatingMode.OFF)),
    HEATER(listOf(HeatingMode.OFF, HeatingMode.HEATING)),
    COOLER(listOf(HeatingMode.OFF, HeatingMode.COOLING)),
    BOTH(HeatingMode.values().toList());


    fun normalizeHeatingMode(desiredHeatingMode: HeatingMode) = when (this) {
        NONE -> HeatingMode.OFF
        BOTH -> desiredHeatingMode
        HEATER -> when (desiredHeatingMode) {
            HeatingMode.COOLING -> HeatingMode.OFF
            else -> desiredHeatingMode
        }
        COOLER -> when (desiredHeatingMode) {
            HeatingMode.HEATING -> HeatingMode.OFF
            else -> desiredHeatingMode
        }
    }

    fun canUseHeatingMode(heatingMode: HeatingMode) = heatingMode in allowableHeatingModes
}

/**
 * The way to control heating and cooling for the FermBot. This is the **ONLY** way you should attempt
 * to enable or disable heating and cooling. Since the heater and cooler are their own digital devices,
 * nothing inherently prevents both from being enabled at the same time. This actuator ensures that
 * cannot happen. Do not manipulate the heater and cooler directly under any circumstances.
 */
interface TemperatureActuator {
    fun setHeatingMode(heatingMode: HeatingMode): HeatingMode
    val currentHeatingMode : HeatingMode
    val statistics: TemperatureActuatorStatistics
    fun resetStatistics()
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
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(heaterPinName, "Heater"))
    }

    @Bean
    @Singleton
    @Named("cooler")
    @Requires(property="fermbot.cooler.enabled", value="true")
    override fun createCooler(): ActiveHighDigitalOutputDevice {
        logger.debug("Registering cooler on pin {}", coolerPinName)
        return HardwareBackedActiveHighDigitalOutputDevice(gpioManager.provisionDigitalOutputDevice(coolerPinName, "Cooler"))
    }
}
