package fermbot

import java.beans.ConstructorProperties
import java.time.Instant

/**
 * @author Zachary Richards
 */
class Tilt @ConstructorProperties("color", "sg", "temp") constructor(private val color: TiltColors,
      override val specificGravity: Double, override val currentTemp: Double) : Thermometer, Hydrometer {

    val timestamp = Instant.now()!!

    override fun toString(): String {
        return "Tilt(color=$color, specificGravity=$specificGravity, currentTemp=$currentTemp, timestamp=$timestamp)"
    }
}
