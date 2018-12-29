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
    override val statistics = TemperatureActuatorStatistics()

    override fun resetStatistics() = statistics.reset()
    private var heatingModeLastChanged = Instant.now()

    override var currentHeatingMode: HeatingMode = HeatingMode.OFF
        private set

    private val logger = LoggerFactory.getLogger(HardwareBackedTemperatureActuator::class.java)


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