package fermbot.profile

import fermbot.Hydrometer
import fermbot.Temperature
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 *
 * @author Zachary Richards
 * @version 12/12/19
 */
class ProfileController(private val setpoints: List<TemperatureSetpoint>, private var currentSetpointIndex: Int = 0, private val fermentationStart: Instant = Instant.now()) { //TODO is there a better name? this doesn't control the fermentation but it controls which stage of the fermentation we are in

    private val logger = LoggerFactory.getLogger(ProfileController::class.java)

    fun getCurrentSetpointIndex() : Int {
        return currentSetpointIndex
    }

    private var currentStageStart: Instant? = if (currentSetpointIndex == 0) {
        fermentationStart //when did the current fermentation stage start? (we need to be able to say e.g. hold temp "for two days")
    } else { TODO("Persist start of stage information") }

    fun getSetpoint(hydrometer: Hydrometer): TemperatureSetpoint { //change to Hydrometer? in case user doesn't have one configured
        if (isCurrentStageFulfilled(hydrometer)) {
            currentSetpointIndex++
        }
        return currentStage
    }

    private fun isCurrentStageFulfilled(hydrometer: Hydrometer): Boolean {
        return when (currentStage) {
            is SpecificGravityBasedSetpoint -> {
               hydrometer.specificGravity <= (currentStage as SpecificGravityBasedSetpoint).untilSg
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
