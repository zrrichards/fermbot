package fermbot.profile

import fermbot.Temperature
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo

/**
 *
 * @author Zachary Richards
 * @version 12/27/19
 */
class TemperatureWindowSpec {

    @Test
    fun `a temperature window of zero is zero regardless of the units used`() {
        val zeroF = TemperatureWindow(0.0, Temperature.Unit.FAHRENHEIT)
        val zeroC = TemperatureWindow(0.0, Temperature.Unit.CELSIUS)
        expectThat(zeroF.get(Temperature.Unit.CELSIUS)).isEqualTo(zeroC.get(Temperature.Unit.FAHRENHEIT))
    }

    @Test
    fun `test conversion`() {
        val oneC = TemperatureWindow(1.0, Temperature.Unit.CELSIUS)
        val oneF = TemperatureWindow(1.8, Temperature.Unit.FAHRENHEIT)

        expectThat(oneC.compareTo(oneF)).isEqualTo(0)
    }

    @Test
    fun `test fromString`() {
        val str = "1.5C"
        val expectedWindow = TemperatureWindow(1.5, Temperature.Unit.CELSIUS)
        expectThat(fromString(str)).isEqualTo(expectedWindow)
    }

    @Test
    fun `test fromString with invalid format`() {
        val str = "1.5"
        expectThrows<IllegalArgumentException> { fromString(str) }
    }

    @Test
    fun `test fromString with invalid unit`() {
        val str = "1.5B"
        expectThrows<IllegalArgumentException> { fromString(str) }
    }
}
