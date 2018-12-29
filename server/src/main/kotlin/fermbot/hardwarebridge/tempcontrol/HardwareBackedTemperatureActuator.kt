package fermbot.hardwarebridge.tempcontrol

import fermbot.monitor.HeatingMode
import fermbot.profile.BeanDefinitions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This class is responsible for actuating the heating and cooling devices. It knows nothing about temperature setpoints.
 * Its primary role is to ensure that the heater and cooler are both not enabled at the same time.
 */
@Singleton
class HardwareBackedTemperatureActuator @Inject constructor(@param:Named(BeanDefinitions.HEATER) private val heater: Optional<ActiveHighDigitalOutputDevice>, @param:Named(BeanDefinitions.COOLER) private val cooler: Optional<ActiveHighDigitalOutputDevice>, private val heatingCoolingConfiguration: HeaterCoolerConfiguration) : TemperatureActuator {

    private var heatingModeLastChanged = Instant.now()

    override val currentHeatingMode: HeatingMode
        get() = determineHeatingModeFromHardware()

    private var previousHeatingMode = currentHeatingMode

    private fun determineHeatingModeFromHardware(): HeatingMode {


        val isHeaterOn = heater.isEnabled()
        val isCoolerOn = cooler.isEnabled()
        if (isHeaterOn && isCoolerOn) {
            logger.error("Heater and cooler both enabled. Attempting to disable")
            heater.get().disable()
            cooler.get().disable()
            throw IllegalStateException("Heater and cooler both enabled. Report this issue on Github Immediately")
        }

        return when {
            isHeaterOn -> HeatingMode.HEATING
            isCoolerOn -> HeatingMode.COOLING
            else -> HeatingMode.OFF
        }
    }

    private val logger = LoggerFactory.getLogger(HardwareBackedTemperatureActuator::class.java)

    private fun Optional<ActiveHighDigitalOutputDevice>.isEnabled() = map { it.isEnabled() }.orElse(false)

    init {
        check (currentHeatingMode == HeatingMode.OFF) { "Heating mode should initially be off. This is a bug" }
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

        previousHeatingMode = currentHeatingMode

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
                    Thread.sleep(100)
                }
                heater.ifPresent { it.enable() }
            }
            HeatingMode.COOLING -> {
                heater.ifPresent {
                    it.disable()
                    Thread.sleep(100)
                }
                cooler.ifPresent { it.enable() }
            }
        }

        //this can only happen due to a programming error but check anyway
        if (heater.isEnabled() && cooler.isEnabled()) {
            heater.get().disable()
            cooler.get().disable()
            throw IllegalStateException("Both Heater and cooler enabled simultaneously. Disabling both. This is a programming error. Please report this issue on github immediately")
        }

        val now = Instant.now()
        val elapsed = Duration.between(heatingModeLastChanged, now)
        logger.info("Changing heating mode from $previousHeatingMode to $currentHeatingMode. Was in mode '$previousHeatingMode for $elapsed")
        heatingModeLastChanged = now
        return previousHeatingMode
    }

}


