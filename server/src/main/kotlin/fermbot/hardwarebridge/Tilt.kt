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
open class Tilt @ConstructorProperties("color", "sg", "temp") constructor(private val color: TiltColors,
                                                                     override val specificGravity: Double, rawTemp: Double, override val timestamp: Instant = Instant.now()) : ThermoHydrometer {

    override val currentTemp = rawTemp.toF() //Tilt readings are always given in Fahrenheit

    override val id = "Tilt[${color.name}]"

    override fun toString(): String {
        return "Tilt(color=$color, specificGravity=$specificGravity, currentTemp=$currentTemp, timestamp=$timestamp)"
    }
}

object NullTilt : ThermoHydrometer {
    override val specificGravity: Double
        get() = throw UnsupportedOperationException("Cannot read specificGravity from Null Tilt")

    override val timestamp: Instant
        get() = throw UnsupportedOperationException("Cannot read timestamp from Null Tilt")

    override val currentTemp: Temperature
        get() = throw UnsupportedOperationException("Cannot read currentTemp from Null Tilt")

    override val id = "Null-Tilt"
}

interface ThermoHydrometer : Thermometer, Hydrometer
