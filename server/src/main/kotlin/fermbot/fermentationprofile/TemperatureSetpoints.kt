package fermbot.fermentationprofile

import fermbot.Temperature
import java.time.Duration


interface TemperatureSetpoint {
    val stageDescription: String
    val tempSetpoint: Temperature
}

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class SpecificGravityBasedSetpoint(override val tempSetpoint: Temperature, val untilSg: Double, override val stageDescription: String = "") : TemperatureSetpoint


data class TimeBasedSetpoint(override val tempSetpoint: Temperature, val duration: Duration, override val stageDescription: String = "", val includeRamp: Boolean) : TemperatureSetpoint
