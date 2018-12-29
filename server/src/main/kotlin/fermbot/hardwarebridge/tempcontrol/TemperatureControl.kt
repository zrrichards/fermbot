package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
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

@Factory
class HeatingCoolingConfigurationFactory {

    @Bean
    @Singleton
    fun determineHeatingCoolingConfiguration(@Named("heater") heater: Optional<ActiveHighDigitalOutputDevice>, @Named("cooler") cooler: Optional<ActiveHighDigitalOutputDevice>): HeaterCoolerConfiguration {
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

/**
 * This class is responsible for actuating the heating and cooling devices. It knows nothing about temperature setpoints.
 * Its primary role is to ensure that the heater and cooler are both not enabled at the same time.
 */
@Singleton
class HardwareBackedTemperatureActuator @Inject constructor(@param:Named("heater") private val heater: Optional<ActiveHighDigitalOutputDevice>, @param:Named("cooler") private val cooler: Optional<ActiveHighDigitalOutputDevice>, private val heatingCoolingConfiguration: HeaterCoolerConfiguration) : TemperatureActuator{
    override val statistics = TemperatureActuatorStatistics()

    override fun resetStatistics() = statistics.reset()
    private var heatingModeLastChanged = Instant.now()

    private var currentHeatingMode = HeatingMode.OFF

    private val logger = LoggerFactory.getLogger(HardwareBackedTemperatureActuator::class.java)

    @Synchronized override fun getCurrentHeatingMode(): HeatingMode = currentHeatingMode

    init {
        logger.info("Initializing Temperature Actuator. Heating Configuration: $heatingCoolingConfiguration. Heating mode is currently $currentHeatingMode")
        if (heatingCoolingConfiguration == HeaterCoolerConfiguration.NONE) {
            logger.warn("No temperature control devices are enabled. The FermBot will not be able to control your fermentation temperature. It will only be monitored. Ensure this is what you want before proceeding")
        }
    }

    /**
     * Sets the current heating mode
     * Returns the previous heating mode
     */
    @Synchronized override fun setHeatingMode(heatingMode: HeatingMode) : HeatingMode {
        check(heatingMode != currentHeatingMode) {
            "Heating mode already set to $heatingMode"
        }

        check(heatingCoolingConfiguration.canUseHeatingMode(heatingMode)) {
            "Heating Cooling Configuration: $heatingCoolingConfiguration, cannot use heating mode $heatingMode"
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

        val now = Instant.now()
        val elapsed = Duration.between(heatingModeLastChanged, now)
        heatingModeLastChanged = now

        val prevHeatingMode = currentHeatingMode
        currentHeatingMode = heatingMode
        statistics.addTime(elapsed, currentHeatingMode)
        return prevHeatingMode
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
    fun getCurrentHeatingMode(): HeatingMode //todo change to val
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
