package fermbot.hardwarebridge
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

import fermbot.Hydrometer
import fermbot.Temperature
import fermbot.Thermometer
import fermbot.toF
import java.beans.ConstructorProperties
import java.time.Instant

/**
 * @author Zachary Richards
 */
class Tilt @ConstructorProperties("color", "specificGravity", "temp") constructor(private val color: TiltColors,
                                                                     override val specificGravity: Double, temp: Double) : ThermoHydrometer {

    override val currentTemp = temp.toF() //Tilt readings are always given in Fahrenheit

    override val timestamp = Instant.now()!!

    override val id = "Tilt[${color.name}]"

    override fun toString(): String {
        return "Tilt(color=$color, specificGravity=$specificGravity, currentTemp=$currentTemp, timestamp=$timestamp)"
    }
}

interface ThermoHydrometer : Thermometer, Hydrometer

