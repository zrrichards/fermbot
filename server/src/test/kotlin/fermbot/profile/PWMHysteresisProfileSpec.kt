package fermbot.profile

import fermbot.Temperature
import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.toF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 *
 * @author Zachary Richards
 * @version 1/10/20
 */
class PWMHysteresisProfileSpec {

    private val profile = PWMHeaterHysteresisProfile(TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT), TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT), HeaterCoolerConfiguration.HEATER)

    @Test
    fun `if temp is at lower bound, duty cycle is 100%`() {
        val setpoint = 48.0.toF()
        val temp = setpoint - profile.lowerThreshold
        expectThat(profile.determineDesiredDutyCycle(setpoint, temp)).isEqualTo(1.0)
    }

    @Test
    fun `if temp is below lower bound, duty cycle is 100%`() {
        val setpoint = 48.0.toF()
        val temp = setpoint - profile.lowerThreshold - 1.0.toF()
        expectThat(profile.determineDesiredDutyCycle(setpoint, temp)).isEqualTo(1.0)
    }

    @Test
    fun `if temp is at the setpoint, duty cycle is 0%`() {
        val setpoint = 48.0.toF()
        val temp = setpoint
        expectThat(profile.determineDesiredDutyCycle(setpoint, temp)).isEqualTo(0.0)
    }

    @Test
    fun `if temp is at midpoint, duty cycle is 50%`() {
        val setpoint = 48.0.toF()
        val temp = setpoint - profile.lowerThreshold / 2.0
        expectThat(profile.determineDesiredDutyCycle(setpoint, temp)).isEqualTo(0.5)
    }
}