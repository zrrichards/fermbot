package fermbot

import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
@Disabled("Not implemented yet")
@MicronautTest
class StateControllerSpec {

    @Inject
    private lateinit var stateController: StateController

    @Test
    fun `initial state is pending profile`() {
        expectThat(stateController.getCurrentState()).isEqualTo(State.PENDING_PROFILE)
    }

    @Test
    fun `pending profile to ready`() {
        expectThat(stateController.getCurrentState()).isEqualTo(State.PENDING_PROFILE)
        stateController.profile(listOf())
        expectThat(stateController.getCurrentState()).isEqualTo(State.READY)
    }

    @Test
    fun `ready to pending profile`() {
        stateController.profile(listOf())
        stateController.reset()
        expectThat(stateController.getCurrentState()).isEqualTo(State.PENDING_PROFILE)
    }

    fun `ready to running`() {}
    fun `running to ready`() {}
    fun `running to pending profile`() {}
}