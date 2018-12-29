package fermbot.profile

import fermbot.Temperature
import fermbot.orchestrator.toPrettyString
import java.time.Duration


sealed class TemperatureSetpoint(open val tempSetpoint: Temperature, open val stageDescription: String)


/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class SpecificGravityBasedSetpoint(override val tempSetpoint: Temperature, val untilSg: Double, override val stageDescription: String = "") : TemperatureSetpoint(tempSetpoint, stageDescription) {
    override fun toString(): String {
        return "$stageDescription[$tempSetpoint until SG $untilSg]"
    }
}


data class TimeBasedSetpoint(override val tempSetpoint: Temperature, val duration: Duration, override val stageDescription: String = "", val includeRamp: Boolean) : TemperatureSetpoint(tempSetpoint, stageDescription) {
    init {
        check (includeRamp) { "Ignoring ramp time currently not supported" }
    }
    override fun toString(): String {
        return "$stageDescription[$tempSetpoint for ${duration.toPrettyString()} includeRamp=$includeRamp]" //todo actually support includeRamp
    }
}
