package fermbot.profile

import fermbot.Temperature
import fermbot.orchestrator.toPrettyString
import java.time.Duration


sealed class TemperatureSetpoint(open val temperature: Temperature, open val description: String)


/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class SpecificGravityBasedSetpoint(override val temperature: Temperature, val untilSg: Double, override val description: String = "") : TemperatureSetpoint(temperature, description) {
    override fun toString(): String {
        return "$description[$temperature until SG $untilSg]"
    }
}


data class TimeBasedSetpoint(override val temperature: Temperature, val duration: Duration, override val description: String = "", val includeRamp: Boolean) : TemperatureSetpoint(temperature, description) {
    init {
        check (includeRamp) { "Ignoring ramp time currently not supported" }
    }
    override fun toString(): String {
        return "$description[$temperature for ${duration.toPrettyString()} includeRamp=$includeRamp]" //todo actually support includeRamp
    }
}
