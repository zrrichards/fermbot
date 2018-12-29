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

    override val currentTemp = fahrenheit(rawTemp)

    override val timestamp = Instant.now()!!

    override fun toString(): String {
        return "Tilt(color=$color, specificGravity=$specificGravity, currentTemp=$currentTemp, timestamp=$timestamp)"
    }
}

