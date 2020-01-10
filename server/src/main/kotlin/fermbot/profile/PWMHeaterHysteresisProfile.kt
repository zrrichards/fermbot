package fermbot.profile

import fermbot.Temperature
import fermbot.Thermometer
import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.monitor.HeatingMode
import fermbot.monitor.HeatingMode.*
import io.micronaut.context.annotation.Value
import org.slf4j.LoggerFactory
import java.lang.Double.min
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 1/10/20
 */
@Singleton
class PWMHeaterHysteresisProfile @Inject constructor(@Value("\${fermbot.hysteresis.lower:1F}") override val lowerThreshold: TemperatureWindow, @Value("\${fermbot.hysteresis.upper:1F}") override val upperThreshold: TemperatureWindow, private val heatingCoolingConfiguration: HeaterCoolerConfiguration) : HysteresisProfile {

    private val minimumOnTime = Duration.ofMinutes(1)
    private val minimumOffTime = Duration.ofMinutes(1)

    private var previousHeatingMode = OFF

    private val logger = LoggerFactory.getLogger(PWMHeaterHysteresisProfile::class.java)

    init {
        logger.info("Initializing Pulse Width Modulated Heating Hysteresis profile. lowerThreshold=$lowerThreshold, upperThreshold=$upperThreshold, minimumOnTime=$minimumOnTime, minimumOffTime=$minimumOffTime")
        check(!heatingCoolingConfiguration.canUseHeatingMode(COOLING)) {
            "Pulse Width Modulation not supported with cooling enabled"
        }
    }

    /**
     * when heating was last changed from OFF to HEATING
     */
    private var lastActivated: Instant? = null

    /**
     * when heating was last changed from HEATING to OFF
     */
    private var lastDeactivated = Instant.now()

    /*
     update current duty cycle each time temperature control task is run and return a desired heating mode accordingly
     */
    override fun determineHeatingMode(setpoint: Temperature, thermometer: Thermometer, currentHeatingMode: HeatingMode): HeatingMode {
        val desiredDutyCycle = determineDesiredDutyCycle(setpoint, thermometer.currentTemp)
        val now = Instant.now()
        val currentDutyCycle = if (lastActivated == null) { 0.0 } else when (currentHeatingMode) {
            COOLING -> throw IllegalStateException("Pulse width modulation not supported with cooling yet")
            HEATING -> Duration.between(lastActivated, now) / Duration.between(lastDeactivated, now)
            OFF -> Duration.between(lastActivated, lastDeactivated) / Duration.between(lastActivated, now)
        }
        val newHeatingMode = when (desiredDutyCycle) {
            1.0 -> HEATING
            0.0 -> OFF
            else -> when {
                currentDutyCycle < desiredDutyCycle -> when {
                    Duration.between(lastDeactivated, now) >= minimumOffTime -> HEATING
                    else -> OFF
                }
                else -> when {
                    Duration.between(lastActivated, now) >= minimumOnTime -> OFF
                    else -> HEATING
                }
            }
        }

        check (newHeatingMode != COOLING)

        if (newHeatingMode != currentHeatingMode) {
            if (newHeatingMode == HEATING) {
                lastActivated = now
            } else {
                lastDeactivated = now
            }
            logger.info("Current Temp ${thermometer.currentTemp}, Setpoint: $setpoint. Previous heating mode: $currentHeatingMode, Desired Duty Cycle: $desiredDutyCycle, Current Duty Cycle: $currentDutyCycle. New Heating Mode: $newHeatingMode")
        }


        return newHeatingMode
    }
}

operator fun TemperatureWindow.div(lowerThreshold: TemperatureWindow) = get(unit) / lowerThreshold.get(unit)
operator fun TemperatureWindow.div(value: Double) = TemperatureWindow(get(unit) / value, unit)

fun PWMHeaterHysteresisProfile.determineDesiredDutyCycle(setpoint: Temperature, currentTemp: Temperature) = if (currentTemp > setpoint) {
    0.0
} else {
    min((setpoint - currentTemp).toTemperatureWindow() / lowerThreshold, 1.0)
}


