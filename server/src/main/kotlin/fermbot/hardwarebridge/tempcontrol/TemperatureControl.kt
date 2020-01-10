package fermbot.hardwarebridge.tempcontrol

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import fermbot.monitor.HeatingMode
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/20/19
 */
interface DigitalOutputDevice {

    fun isEnabled(): Boolean
    fun enable()
    fun disable()
    fun isDisabled() = !isEnabled()
}

class HardwareBackedActiveLowDigitalOutputDevice(private val outputPin: DigitalOutput) : DigitalOutputDevice {
    override fun isEnabled(): Boolean {
        return outputPin.isLow()
    }

    override fun enable() {
        outputPin.setLow()
    }

    override fun disable() {
        outputPin.setHigh()
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
}

@Factory
class HardwareBackedHeaterCoolerFactory @Inject constructor(private val gpioManager: GpioManager) : HeaterCoolerFactory {

    private val logger = LoggerFactory.getLogger(HardwareBackedHeaterCoolerFactory::class.java)

    init {
        logger.info("Initializing Heater and cooler factory")
    }

    private var heaterPinName: String? = null
    private var coolerPinName: String? = null

    @Bean
    @Singleton
    @Named("heater")
    @Requires(property = FermbotProperties.isHeaterEnabled, value="true")
    override fun createHeater(@Property(name=FermbotProperties.heaterPinName) heaterPinName: String): DigitalOutputDevice {

        if (coolerPinName != null && heaterPinName == coolerPinName) {
            throw IllegalArgumentException("Both heater and cooler defined on pin $heaterPinName. They must be on different pins")
        }

        this.heaterPinName = heaterPinName

        logger.debug("Registering heater on pin {}", heaterPinName)
        return HardwareBackedActiveLowDigitalOutputDevice(gpioManager.provisionActiveLowDigitalOutput(heaterPinName, "Heater"))
    }

    @Bean
    @Singleton
    @Named("cooler")
    @Requires(property = FermbotProperties.isCoolerEnabled, value="true")
    override fun createCooler(@Property(name=FermbotProperties.coolerPinName) coolerPinName: String): DigitalOutputDevice {

        if (heaterPinName != null && coolerPinName == heaterPinName) {
            throw IllegalArgumentException("Both heater and cooler defined on pin $heaterPinName. They must be on different pins")
        }

        this.coolerPinName = coolerPinName
        logger.debug("Registering cooler on pin {}", coolerPinName)
        return HardwareBackedActiveLowDigitalOutputDevice(gpioManager.provisionActiveLowDigitalOutput(coolerPinName, "Cooler"))
    }
}
