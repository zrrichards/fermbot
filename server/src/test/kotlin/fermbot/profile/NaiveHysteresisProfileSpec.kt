package fermbot.profile

import fermbot.Temperature
import fermbot.Thermometer
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.monitor.HeatingMode.*
import fermbot.profile.HysteresisStatus.*
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
class NaiveHysteresisProfileSpec {

    @Test
    fun `if temp is within hysteresis range then off is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = setpoint + threshold
        val thermometer = thermo(temp)
//        expectThat(profile.determineHeatingMode(setpoint, thermometer)).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `test hysteresis status to heating mode mapping`() {
        val expected = mutableMapOf(
                (MAX to HEATING) to COOLING,
                (MAX to COOLING) to COOLING,
                (MAX to OFF) to COOLING,

                (ABOVE to HEATING) to OFF,
                (ABOVE to COOLING) to COOLING,
                (ABOVE to OFF) to COOLING,

                (WITHIN to HEATING) to HEATING,
                (WITHIN to COOLING) to COOLING,
                (WITHIN to OFF) to OFF,

                (BELOW to HEATING) to HEATING,
                (BELOW to COOLING) to OFF,
                (BELOW to OFF) to HEATING,

                (MIN to HEATING) to HEATING,
                (MIN to COOLING) to HEATING,
                (MIN to OFF) to HEATING
        )

        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()

        fun getOffset(status: HysteresisStatus): Temperature {
            return when (status) {
                MIN -> setpoint - profile.min
                BELOW -> setpoint - profile.lowerThreshold
                WITHIN -> setpoint
                ABOVE -> setpoint + profile.upperThreshold
                MAX -> setpoint + profile.max
            }
        }

        expected.forEach {
            print("Testing that temp is at ${it.key.first} current mode is ${it.key.second} -> ${it.value} ... ")
            val temp = getOffset(it.key.first)
            expectThat(profile.determineHeatingMode(setpoint, thermo(temp), it.key.second)).isEqualTo(it.value)
            println("PASSED")
        }
    }

    private fun thermo(temp: Temperature) = DS18B20("foo-test-id", temp)

    @Test
    fun `when temperature is equal max, then max is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = profile.max + setpoint
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(MAX)
    }

    @Test
    fun `when temperature is above max, then max is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = profile.max + setpoint + 1.0.toF()
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(MAX)
    }

    @Test
    fun `when temperature is equal min, then min is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = setpoint - profile.min
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(HysteresisStatus.MIN)
    }

    @Test
    fun `when temperature is above min, then min is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = setpoint - profile.min - 1.0.toF()
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(HysteresisStatus.MIN)
    }

    @Test
    fun `when temperature is equal upperThreshold then above is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = profile.upperThreshold + setpoint
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(HysteresisStatus.ABOVE)
    }

    @Test
    fun `when temperature is equal lowerThreshold, then below is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = setpoint - profile.lowerThreshold
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(HysteresisStatus.BELOW)
    }

    @Test
    fun `when temp is between upper and lower, within is returned`() {
        val threshold = TemperatureWindow(1.0, Temperature.Unit.FAHRENHEIT)
        val profile = symmetricNaiveHysteresisProfile(threshold, HeaterCoolerConfiguration.BOTH)
        val setpoint = 45.5.toF()
        val temp = setpoint
        expectThat(profile.getHysteresisStatus(setpoint, temp)).isEqualTo(HysteresisStatus.WITHIN)
    }

}