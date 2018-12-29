package fermbot.ds18b20

import fermbot.toC
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
class TemperatureCorrectorSpec {

    @Test
    fun `test temperature correction equation`() {
        val a = listOf(4, 3, 5, 7)
        val b = listOf(8, 7, 4, 7)
        val c = listOf(5, 6, 2, 5)

        val x = listOf(23, 24, 30, 25)

        val y = listOf(2305, 1902, 4622, 4555)

        //y = ax^2 + bx + c

        for (i in 0 until x.size) {
            expectThat(CustomDS18B20TemperatureCorrector(a[i].toDouble(), b[i].toDouble(), c[i].toDouble(), 0.0.toC(), 100.0.toC())(x[i].toDouble().toC()))
                    .isEqualTo(y[i].toDouble().toC())
        }
    }
}