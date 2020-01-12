package fermbot.profile

import fermbot.Hydrometer
import fermbot.monitor.FermentationMonitorTask
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.roundToLong

/**
 *
 * @author Zachary Richards
 * @version 12/12/19
 */
class SetpointDeterminer(private val setpoints: List<TemperatureSetpoint>, private val setpointCompletionPersister: Persister<SetpointCompletion>, val fermentationMonitorTask: FermentationMonitorTask) {

    private val logger = LoggerFactory.getLogger(SetpointDeterminer::class.java)

    private var currentSetpointCompletion = setpointCompletionPersister.loadOrElse(beginning())

    private fun beginning() = SetpointCompletion(setpoints[0], 0)

    val currentSetpointIndex: Int
        get() = currentSetpointCompletion.currentSetpointIndex

    init {
        logger.info("Initializing SetpointDeterminer")
        if (currentSetpointCompletion.currentSetpointIndex > setpoints.lastIndex) {
            throw IllegalStateException("Previous fermentation has been completed. Please set a new profile. Cannot start an already completed profile")
        }
        val setpointFromProfile = setpoints[currentSetpointCompletion.currentSetpointIndex]
        if (currentSetpointCompletion.setpont != setpointFromProfile) {
            logger.info("Setpoint mismatch from persisted data. Loaded from file: ${currentSetpointCompletion.setpont}. Set in the current profile: $setpointFromProfile. Ignoring persisted data")
            currentSetpointCompletion = beginning()
        }
        setpointCompletionPersister.persist(currentSetpointCompletion)
    }

    fun <T: Hydrometer> getSetpoint(hydrometer: Optional<T>): TemperatureSetpoint {
        if (isCurrentSetpointFulfilled(hydrometer)) {
            advanceToNextSetpoint()
        }
        return currentSetpoint
    }

    private fun <T: Hydrometer> isCurrentSetpointFulfilled(hydrometer: Optional<T>): Boolean {
        return when (currentSetpoint) {
            is SpecificGravityBasedSetpoint -> {
               if (hydrometer.isPresent) {
                   if (isSpecificGravityBasedSetpointReached(hydrometer.get())) {
                       logger.info("Specific gravity of ${hydrometer.get().specificGravity} satisfies the current setpont: $currentSetpoint")
                      true
                   } else {
                       logger.debug("Specific gravity of ${hydrometer.get().specificGravity} does not satisfy the current setpont: $currentSetpoint")
                       false
                   }
               } else {
                   throw IllegalStateException("Specific gravity based setpoint in use but no hydrometer found")
               }
            }
            is TimeBasedSetpoint -> {
                val elapsed = Duration.between(currentSetpointCompletion.previousSetpointCompletionTime, Instant.now())
                if (elapsed >= (currentSetpoint as TimeBasedSetpoint).duration) {
                    logger.info("Duration of $elapsed satisfies current setpoint: ${(currentSetpoint as TimeBasedSetpoint).duration}")
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun isSpecificGravityBasedSetpointReached(hydrometer: Hydrometer): Boolean {
        val untilSg = (currentSetpoint as SpecificGravityBasedSetpoint).untilSg
        val currentGravityReached =  hydrometer.specificGravity <= untilSg
        val pastGravityReached = fermentationMonitorTask.averageGravityFromPast(Duration.ofHours(6)) <= untilSg
        if (currentGravityReached) {
            logger.info("Current gravtiy of $hydrometer satisfies setpoint value")
            return if (pastGravityReached) {
                logger.info("Past gravity satisfies setpoint. Current setpoint complete")
                true
            } else {
                logger.info("Past gravity does not satisfy setpoint. Current setpoint not complete")
                false
            }
        }

        return false

    }

    fun getRemainingSetpointInfo() : String {
        val elapsed = Duration.between(currentSetpointCompletion.previousSetpointCompletionTime, Instant.now())
        if (currentSetpointIndex == setpoints.size) {
            return "Fermentation complete"
        }
        return when (currentSetpoint) {
            is TimeBasedSetpoint -> {
                val duration = (currentSetpoint as TimeBasedSetpoint).duration
                val remainingTime = duration - elapsed
                val percentComplete = elapsed / duration * 100
                """$remainingTime remaining for current setpoint ("${currentSetpoint.description}") duration of $duration ($percentComplete% complete)"""
            }
            is SpecificGravityBasedSetpoint -> {
                "TODO SG based setpoint data"
            }
        }
    }

    private fun SetpointCompletion.toNextSetpoint(): SetpointCompletion {
        val nextSetpoint = if (currentSetpointIndex >= setpoints.lastIndex) {
            null
        } else {
            setpoints[currentSetpointIndex + 1]
        }
        return SetpointCompletion(nextSetpoint, this.currentSetpointIndex + 1)
    }

    fun advanceToNextSetpoint() {
        val setpointName = currentSetpoint.description.defaultIfEmpty("$currentSetpointIndex")
        val lastSetpointCompleted = currentSetpointIndex >= setpoints.lastIndex
        if (lastSetpointCompleted) {
            logger.warn("You are currently at the last setpoint and it has been completed. Continuing to hold temp constant at the current setpoint (${currentSetpoint.temperature}). Is your batch done?")
        } else {
            logger.info("""Fermentation setpoint "$setpointName" fulfilled. Moving to next setpoint""")
        }

        currentSetpointCompletion = currentSetpointCompletion.toNextSetpoint()
        fermentationMonitorTask.run()
        setpointCompletionPersister.persist(currentSetpointCompletion)
    }

    val currentSetpoint
        get() = if (currentSetpointIndex >= setpoints.size) { setpoints.last() } else { setpoints[currentSetpointIndex] }
}

operator fun Duration.div(other: Duration) = toMillis().toDouble() / other.toMillis().toDouble()
operator fun Duration.div(other: Double) = Duration.ofMillis((toMillis().toDouble() / other).roundToLong())!!

private fun String.defaultIfEmpty(s: String) = if (isEmpty()) { s } else { this }

/**
 * This stores information about the current setpoint. Namely:
 * 1. The setpoint we are currently on
 * 2. Its index in the profile in the profile
 * 2. At what time was the previous setpoint completed (i.e. setpoint -1)
 *   -We need this info so we can do setpoints for a specific amount of time (i.e. 2 days)
 */
data class SetpointCompletion(val setpont: TemperatureSetpoint?, val currentSetpointIndex: Int, val previousSetpointCompletionTime: Instant = Instant.now())

