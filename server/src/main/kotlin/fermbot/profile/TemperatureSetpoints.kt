package fermbot.profile

import fermbot.Temperature
import java.time.Duration


sealed class TemperatureSetpoint(open val tempSetpoint: Temperature, open val stageDescription: String)


/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class SpecificGravityBasedSetpoint(override val tempSetpoint: Temperature, val untilSg: Double, override val stageDescription: String = "") : TemperatureSetpoint(tempSetpoint, stageDescription)


data class TimeBasedSetpoint(override val tempSetpoint: Temperature, val duration: Duration, override val stageDescription: String = "", val includeRamp: Boolean) : TemperatureSetpoint(tempSetpoint, stageDescription)
