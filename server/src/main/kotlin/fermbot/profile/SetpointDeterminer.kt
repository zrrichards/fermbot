package fermbot.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import fermbot.Hydrometer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/12/19
 */
class SetpointDeterminer(private val setpoints: List<TemperatureSetpoint>, private var currentSetpointIndex: Int = 0, private val fermentationStart: Instant = Instant.now()) { //TODO is there a better name? this doesn't control the fermentation but it controls which stage of the fermentation we are in

    @JsonIgnore
    private val logger = LoggerFactory.getLogger(SetpointDeterminer::class.java)

    fun getCurrentSetpointIndex() = currentSetpointIndex

    private val fermentationStagesStart = mutableMapOf<Int, Instant>()

    fun <T: Hydrometer> getSetpoint(hydrometer: Optional<T>): TemperatureSetpoint {
        if (isCurrentStageFulfilled(hydrometer)) {
            logger.info("Fermentation stage index[$currentSetpointIndex] fulfilled. Moving to next stage")
            currentSetpointIndex++
        }
        return currentStage
    }

    private fun hasGravityBasedSetpoint(setpoints: List<TemperatureSetpoint>): Boolean {
        return setpoints.any { it is SpecificGravityBasedSetpoint }
    }

    private fun <T: Hydrometer> isCurrentStageFulfilled(hydrometer: Optional<T>): Boolean {
        return when (currentStage) {
            is SpecificGravityBasedSetpoint -> {
               if (hydrometer.isPresent) {
                   hydrometer.get().specificGravity <= (currentStage as SpecificGravityBasedSetpoint).untilSg
               } else {
                   throw IllegalStateException("Specific gravity based setpoint in use but no hydrometer found")
               }
            }
            is TimeBasedSetpoint -> {
               false
            }
        }
    }

    private val currentStage
        get() = setpoints[currentSetpointIndex]
}
//logger.warn("You are currently at the last setpoint and it has been completed. Continuing to hold temp constant at the current setpoint (${currentSetpoint.tempSetpoint}). Is your batch done?")
