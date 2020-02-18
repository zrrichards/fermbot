package fermbot

import fermbot.brewfather.ConfigurationValidator
import fermbot.monitor.FermentationMonitorTask
import fermbot.profile.BeanDefinitions
import fermbot.profile.FermentationProfileController
import fermbot.profile.Persister
import fermbot.profile.TemperatureSetpoint
import io.micronaut.context.annotation.Context
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Named

/**
 * The main controller for the Fermbot. This controls all state changes and is the main entry point for interaction.
 * This is a context bean so that it is loaded at startup and the current state information is initialized
 * @author Zachary Richards
 * @version 12/30/19
 */
@Context
@Controller("/")
class StateController @Inject constructor (private val fermentationProfileController: FermentationProfileController, private val configurationValidator: ConfigurationValidator, @param:Named(BeanDefinitions.STATE_PERSISTER) private val statePersister: Persister<State>) {

    private val logger = LoggerFactory.getLogger(StateController::class.java)

    private var currentState: State

    @Get("/state")
    fun getCurrentState() = currentState

    init {
        if (!configurationValidator.isUsable()) {
            currentState = State.PENDING_PROFILE
            returnToPendingProfile()
            logger.warn("Configuration is not usable for fermentation. See message above. Fermbot is in test only mode")
        } else {
            if (fermentationProfileController.isProfileSet()) {
                currentState = State.READY
            } else {
                currentState = State.PENDING_PROFILE
            }
            logger.info("Initializing state controller. Current state: $currentState")
        }
        val desiredState = statePersister.loadOrElse(currentState)
        if (desiredState == State.RUNNING) {
            logger.info("Was in state 'running' before restart. Starting")
            start()
            logger.info("Current Setpoint: ${fermentationProfileController.currentSetpoint}")
        }
    }

    @Post("/profile")
    fun profile(@Body setpoints: List<TemperatureSetpoint>) {
        checkState("Profile", State.PENDING_PROFILE)
        fermentationProfileController.setProfile(setpoints)
        setState(State.READY)
    }

    @Post("/start")
    fun start() {
        checkState("Start", State.READY)
        fermentationProfileController.start()
        setState(State.RUNNING)
    }

    @Post("/pause")
    fun pause() {
        checkState("Pause", State.RUNNING)
        fermentationProfileController.cancel()
        setState(State.READY)
    }

    @Post("/cancel")
    fun cancel() {
        checkState("Cancel", State.RUNNING)
        fermentationProfileController.cancel()
        fermentationProfileController.clearProfile()
        setState(State.PENDING_PROFILE)
    }

    @Post("/reset")
    fun reset() {
        checkState("Reset", State.READY)
        fermentationProfileController.clearProfile()
        setState(State.PENDING_PROFILE)
    }

    private fun checkState(operation: String, desiredState: State) {
        check(configurationValidator.isUsable()) {
            "Fermbot is not in a configuration that supports monitoring. Operation not permitted"
        }
        check(currentState == desiredState)  {
            "$operation can only be called when in the ${desiredState.name} state. Current state: $currentState"
        }
    }

    private fun setState(newState: State) {
        logger.info("Changing state: ${currentState.name} -> ${newState.name}")
        currentState = newState
        statePersister.persist(currentState)
    }

    fun returnToPendingProfile() {
        when (currentState) {
            State.READY -> reset()
            State.RUNNING -> cancel()
            State.PENDING_PROFILE -> { /*do nothing */ }
            else -> { throw IllegalStateException("Current state: $currentState") }
        }
    }
}

