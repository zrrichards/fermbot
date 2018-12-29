package fermbot

import fermbot.monitor.FermentationMonitorTask
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

/**
 * The main controller for the Fermbot. This controls all state changes and is the main entry point for interaction.
 * This is a context bean so that it is loaded at startup and the current state information is initialized
 * @author Zachary Richards
 * @version 12/30/19
 */
@Context
@Controller("/")
class StateController @Inject constructor (private val fermentationProfileController: FermentationProfileController) {

    private val logger = LoggerFactory.getLogger(StateController::class.java)

    private var currentState: State

    @Get("/state")
    fun getCurrentState() = currentState

    @Get("/temp/stats")
    fun getTemperatureStatistics() = fermentationProfileController.statistics

    init {
        if (fermentationProfileController.isProfileSet()) {
            currentState = State.READY
        } else {
            currentState = State.PENDING_PROFILE
        }
        logger.info("Initializing state controller. Current state: $currentState")
    }

    @Post("/profile")
    fun profile(@Body setpoints: List<TemperatureSetpoint>) {
        checkState(State.PENDING_PROFILE)
        fermentationProfileController.setProfile(setpoints)
        setState(State.READY)
    }

    @Post("/start")
    fun start() {
        checkState(State.READY)
        fermentationProfileController.start()
        setState(State.RUNNING)
    }

    @Post("/pause")
    fun pause() {
        checkState(State.RUNNING)
        fermentationProfileController.cancel()
        setState(State.READY)
    }

    @Post("/cancel")
    fun cancel() {
        checkState(State.RUNNING)
        fermentationProfileController.cancel()
        fermentationProfileController.clearProfile()
        setState(State.PENDING_PROFILE)
    }

    @Post("/reset")
    fun reset() {
        checkState(State.READY)
        fermentationProfileController.clearProfile()
        setState(State.PENDING_PROFILE)
    }

    private fun checkState(desiredState: State) {
        check(currentState == desiredState)  {
            "Start can only be called when in the ${desiredState.name} state. Current state: $currentState"
        }
    }

    private fun setState(newState: State) {
        logger.info("Changing state: ${currentState.name} -> ${newState.name}")
        currentState = newState
        currentState.persist()
    }

    fun returnToPendingProfile() {
        when (currentState) {
            State.READY -> reset()
            State.RUNNING -> cancel()
            State.PENDING_PROFILE -> { /*do nothing */ }
            else -> { throw IllegalStateException("Current stage: $currentState") }
        }
    }
}

