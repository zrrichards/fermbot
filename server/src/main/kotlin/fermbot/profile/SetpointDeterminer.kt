package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fermbot.Hydrometer
import fermbot.monitor.FermentationMonitorTask
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

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

    private val fermentationStagesStart = listOf<SetpointCompletion>()

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
    }

    fun <T: Hydrometer> getSetpoint(hydrometer: Optional<T>): TemperatureSetpoint {
        if (isCurrentStageFulfilled(hydrometer)) {
            val stageName = currentStage.stageDescription.defaultIfEmpty("$currentSetpointIndex")
            val lastStageCompleted = currentSetpointIndex >= setpoints.lastIndex
            if (lastStageCompleted) {
                logger.warn("You are currently at the last setpoint and it has been completed. Continuing to hold temp constant at the current setpoint (${currentStage.tempSetpoint}). Is your batch done?")
            } else {
                logger.info("""Fermentation stage "$stageName" fulfilled. Moving to next stage""")
            }

            currentSetpointCompletion = currentSetpointCompletion.toNextStage()
            fermentationMonitorTask.run()
            setpointCompletionPersister.persist(currentSetpointCompletion)
        }
        return currentStage
    }

    private fun <T: Hydrometer> isCurrentStageFulfilled(hydrometer: Optional<T>): Boolean {
        return when (currentStage) {
            is SpecificGravityBasedSetpoint -> {
               if (hydrometer.isPresent) {
                   if (isSpecificGravityBasedSetpointReached(hydrometer)) {
                       logger.info("Specific gravity of ${hydrometer.get().specificGravity} satisfies the current setpont: $currentStage")
                      true
                   } else {
                       logger.debug("Specific gravity of ${hydrometer.get().specificGravity} does not satisfy the current setpont: $currentStage")
                       false
                   }
               } else {
                   throw IllegalStateException("Specific gravity based setpoint in use but no hydrometer found")
               }
            }
            is TimeBasedSetpoint -> {
                val elapsed = Duration.between(currentSetpointCompletion.previousSetpointCompletionTime, Instant.now())
                if (elapsed >= (currentStage as TimeBasedSetpoint).duration) {
                    logger.info("Duration of $elapsed satisfies current setpoint: ${(currentStage as TimeBasedSetpoint).duration}")
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun <T : Hydrometer> isSpecificGravityBasedSetpointReached(hydrometer: Optional<T>) =
            hydrometer.get().specificGravity <= (currentStage as SpecificGravityBasedSetpoint).untilSg

    fun getRemainingStageInfo() : String {
        val elapsed = Duration.between(currentSetpointCompletion.previousSetpointCompletionTime, Instant.now())
        return when (currentStage) {
            is TimeBasedSetpoint -> {
                val duration = (currentStage as TimeBasedSetpoint).duration
                val remainingTime = duration - elapsed
                val percentComplete = elapsed / duration * 100
                """$remainingTime remaining for current stage ("${currentStage.stageDescription}") duration of $duration ($percentComplete% complete)"""
            }
            is SpecificGravityBasedSetpoint -> {
                "TODO SG based setpoint data"
            }
        }
    }

    private fun SetpointCompletion.toNextStage(): SetpointCompletion {
        val nextSetpoint = if (currentSetpointIndex == setpoints.lastIndex) {
            null
        } else {
            setpoints[currentSetpointIndex + 1]
        }
        return SetpointCompletion(nextSetpoint, this.currentSetpointIndex + 1)
    }

    val currentStage
        get() = setpoints[currentSetpointIndex]
}

private operator fun Duration.div(other: Duration) = toMillis().toDouble() / other.toMillis().toDouble()

private fun String.defaultIfEmpty(s: String) = if (isEmpty()) { s } else { this }

/**
 * This stores information about the current setpoint. Namely:
 * 1. The setpoint we are currently on
 * 2. Its index in the profile in the profile
 * 2. At what time was the previous stage completed (i.e. setpoint -1)
 *   -We need this info so we can do setpoints for a specific amount of time (i.e. 2 days)
 */
data class SetpointCompletion(val setpont: TemperatureSetpoint?, val currentSetpointIndex: Int, val previousSetpointCompletionTime: Instant = Instant.now())

@Singleton
@Named(BeanDefinitions.SETPOINT_COMPLETION_PERSISTER)
class SetpointCompletionPersister @Inject constructor(private val objectMapper: ObjectMapper) : Persister<SetpointCompletion> {

    private val logger = LoggerFactory.getLogger(FileBasedProfilePersister::class.java)
    private val currentProfileFile = Paths.get(".current-setpoint-completion.json").toFile()
    override fun hasPersistedData() = currentProfileFile.exists() && currentProfileFile.readBytes().isNotEmpty()

    override fun read(): SetpointCompletion {
        logger.info("Reading setpoint completion from: {}", currentProfileFile.absolutePath)
        return objectMapper.readValue<SetpointCompletion>(currentProfileFile)
    }

    override fun persist(currentProfile: SetpointCompletion) {
        logger.info("Persisting setpoint completion to: {}", currentProfileFile.absolutePath)
        currentProfileFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentProfile))
    }

    override fun clear() {
        currentProfileFile.delete()
    }

}