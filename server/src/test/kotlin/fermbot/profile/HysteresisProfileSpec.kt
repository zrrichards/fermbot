package fermbot.profile

import fermbot.Temperature
import fermbot.Thermometer
import fermbot.hardwarebridge.DS18B20
import fermbot.monitor.HeatingMode
import fermbot.toF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
class HysteresisProfileSpec {

    @Test
    fun `can create unit-less hysteresis profile if there is no unit specificed`() { //TODO doc
        val profile = HysteresisProfile(ZERO, ZERO)
        //doesn't throw an exception
    }

    @Test
    fun `if thermometer is not present than heating mode off is returned`() {
        val profile = symmetricHysteresisProfile(TemperatureWindow(0.0, Temperature.Unit.FAHRENHEIT))
        expectThat(profile.determineHeatingMode(65.5.toF(), Optional.empty())).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `if temp is within hysteresis range then off is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricHysteresisProfile(threshold)
        val setpoint = 45.5.toF()
        val temp = setpoint + threshold
        val thermometer = thermoOptional(temp)
        expectThat(profile.determineHeatingMode(setpoint, thermometer)).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `if temp is above hysteresis range then cooling is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricHysteresisProfile(threshold)
        val setpoint = 45.5.toF()
        val temp = setpoint + 1.0.toF() + threshold
        val thermometer = thermoOptional(temp)
        expectThat(profile.determineHeatingMode(setpoint, thermometer)).isEqualTo(HeatingMode.COOLING)
    }

    @Test
    fun `if temp is below hysteresis range then heating is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricHysteresisProfile(threshold)
        val setpoint = 45.5.toF()
        val temp = setpoint - 1.0.toF() - threshold
        val thermometer = thermoOptional(temp)
        expectThat(profile.determineHeatingMode(setpoint, thermometer)).isEqualTo(HeatingMode.HEATING)
    }

    private fun thermoOptional(temp: Temperature): Optional<Thermometer> {
        return DS18B20("foo-test-id", temp).toOptional()
    }
}