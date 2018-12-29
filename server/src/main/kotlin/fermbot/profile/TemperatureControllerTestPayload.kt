package fermbot.profile

import fermbot.monitor.HeatingMode
import java.time.Duration

/**
 * A payload that can be sent to the fermentation controller to test the heating and cooling devices
 */
data class TemperatureControllerTestPayload(val mode: HeatingMode, val duration: Duration)
