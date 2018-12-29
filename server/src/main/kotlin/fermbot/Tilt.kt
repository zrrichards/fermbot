package fermbot
/*  Fermbot - Open source fermentation monitoring software.
 *  Copyright (C) 2019 Zachary Richards
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import fermbot.ds18b20.TemperatureCorrector
import fermbot.ds18b20.checkInBounds
import java.beans.ConstructorProperties
import java.time.Instant

/**
 * @author Zachary Richards
 */
class Tilt @ConstructorProperties("color", "sg", "temp") constructor(private val color: TiltColors,
      override val specificGravity: Double, rawTemp: Double) : Thermometer, Hydrometer {

    override val corrector = NullTemperatureCorrector
    override val currentTemp = corrector(fahrenheit(rawTemp))

    val timestamp = Instant.now()!!

    override fun toString(): String {
        return "Tilt(color=$color, specificGravity=$specificGravity, currentTemp=$currentTemp, timestamp=$timestamp)"
    }
}

/**
 * A temperature corrector that does not adjust the temperature. Useful when the temperature is assumed to be accurate enough
 * or no temperature correction is desired.
 * Valid from 0C to 100C
 */
object NullTemperatureCorrector : TemperatureCorrector {
    override val upperBound = celsius(100)
    override val lowerBound = celsius(0)

    override fun invoke(rawTemp: Temperature): Temperature {
        checkInBounds(rawTemp, this)
        return rawTemp
    }
}