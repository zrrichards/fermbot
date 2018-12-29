package fermbot.ds18b20

import fermbot.Temperature
import fermbot.toTemperatureUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
class TemperaturesSpec {

    @Test
    fun `can get temperature reading in same scale as created`() {
        val temp = Temperature(32.0, Temperature.Unit.FAHRENHEIT)
        assertEquals(32.0, temp.get(Temperature.Unit.FAHRENHEIT))
    }

    @Test
    fun `can create fahrenheit temperature and get it as celcius`() {
        val temp = Temperature(32.0, Temperature.Unit.FAHRENHEIT)
        assertEquals(0.0, temp.get(Temperature.Unit.CELSIUS))
    }

    @Test
    fun `can convert from celsius to fahrenheit`() {
        val temp = Temperature(8.8888, Temperature.Unit.CELSIUS)
        assertEquals(48.0, temp.get(Temperature.Unit.FAHRENHEIT), 0.001)
    }

    @Test
    fun `temperature symbols are correct`() {
        assertEquals("C", Temperature.Unit.CELSIUS.symbol)
        assertEquals("F", Temperature.Unit.FAHRENHEIT.symbol)
    }

    @Test
    fun `can get unit from symbol`() {
        expectThat("C".toTemperatureUnit()).isEqualTo(Temperature.Unit.CELSIUS)
        expectThat("F".toTemperatureUnit()).isEqualTo(Temperature.Unit.FAHRENHEIT)
    }
}