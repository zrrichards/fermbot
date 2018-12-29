package fermbot.monitor

import fermbot.Temperature
import fermbot.profile.TemperatureSetpoint
import java.time.Instant

/**
 *
 * @author Zachary Richards
 * @version 12/17/19
 */
data class FermentationSnapshot(val timestamp: Instant = Instant.now(), val temp: Temperature?, val currentSg: Double?, val heatingMode: HeatingMode, val currentSetpointIndex: Int, val setpoint: Temperature, val stageDescription: String)

enum class HeatingMode {
    HEATING,
    COOLING,
    OFF
}
