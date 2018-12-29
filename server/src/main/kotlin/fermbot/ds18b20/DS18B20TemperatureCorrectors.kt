package fermbot.ds18b20

import fermbot.Temperature
import fermbot.toC

/**
 * From the DS18B20 datasheet we see the following mean error points (in Celsius):
 * (rawReading, error)
 * (0,  -0.14)
 * (20, -0.2)
 * (30, -0.175)
 *
 * These points define a parabola with the equation:
 * y = 1.833333e-4x^2 - 6.66666e-3x - .14
 *
 * valid from approximately 0C to 30C
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
object DefaultDS18B20TemperatureCorrector : TemperatureCorrector {
    override val upperBound = 0.0.toC()
    override val lowerBound = 30.0.toC()

    private val correctorDelegate = CustomDS18B20TemperatureCorrector(1.833333e-4, -6.666666e-3, -0.14, lowerBound, upperBound)

    override fun invoke(rawTemp: Temperature): Temperature {
        return correctorDelegate(rawTemp)
    }
}

/**
 * Custom DS18B20 corrector defined by a, b, and c
 * where
 * f(x) = ax^2 + bx + c given that x is within the upper and lower bound
 * and the parameters are in relation to celsius
 */
class CustomDS18B20TemperatureCorrector(val a: Double, val b: Double, val c: Double, override val lowerBound: Temperature, override val upperBound: Temperature) : TemperatureCorrector {
    override fun invoke(rawTemp: Temperature): Temperature {
        checkInBounds(rawTemp, this)
        val rawTempCelsius = rawTemp.get(Temperature.Unit.CELSIUS)
        val correctedTemp = (a * rawTempCelsius * rawTempCelsius) + (b * rawTempCelsius) + c
        return correctedTemp.toC()
    }

}

fun checkInBounds(rawTemp: Temperature, corrector: TemperatureCorrector) {
    require(rawTemp.get(Temperature.Unit.CELSIUS) in corrector.lowerBound.get(Temperature.Unit.CELSIUS)..corrector.upperBound.get(Temperature.Unit.CELSIUS))
}