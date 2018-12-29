package fermbot

import io.micronaut.context.annotation.Context
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory

/**
 * The main controller for the Fermbot. This controls all state changes and is the main entry point for interaction.
 * This is a context bean so that it is loaded at startup and the current state information is initialized
 * @author Zachary Richards
 * @version 12/30/19
 */
@Context
@Controller("/")
class StateController {

    private val logger = LoggerFactory.getLogger(StateController::class.java)

    private var currentState: State

    fun getCurrentState() = currentState

    init {
        //Load previous data
        currentState = State.PENDING_PROFILE //(or READY if prev data is present)
        logger.info("Initializing state controller. Current state: $currentState")
    }

//    @Post("/profile") currently conflicting with the profile controller. need to migrate that code here
    fun profile() {
        checkState(State.PENDING_PROFILE)
        //do set profile
        setState(State.READY)
    }

    @Post("/start")
    fun start() {
        checkState(State.READY)
        //do start
        setState(State.RUNNING)
    }

    @Post("/pause")
    fun pause() {
        checkState(State.RUNNING)
        //do pause
        setState(State.READY)
    }

    @Post("/cancel")
    fun cancel() {
        checkState(State.RUNNING)
        //do cancel
        setState(State.PENDING_PROFILE)
    }

    @Post("/reset")
    fun reset() {
        checkState(State.READY)
        //do reset
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
}

