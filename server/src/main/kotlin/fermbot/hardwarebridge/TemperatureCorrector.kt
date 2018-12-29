package fermbot.hardwarebridge

import fermbot.Temperature

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
interface TemperatureCorrector {

    /**
     * The upper bound to which this function is applicable (inclusive)
     */
    val upperBound: Temperature

    /**
     * The lower bound to which this function is applicable (inclusive)
     */
    val lowerBound: Temperature

    /**
     * Correct error readings from the DS18B20.
     * Will throw an IllegalArgumentException if the temperature is outside of the bounds (exclusive)
     */
    operator fun invoke(rawTemp: Temperature): Temperature
}