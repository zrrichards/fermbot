package fermbot

import fermbot.State.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
class StateSpec {

    @Test
    fun `valid next stages from PENDING_PROFILE`() {
        val sut = PENDING_PROFILE
        expectThat(sut.name).isEqualTo("Pending Profile")
        val expectedNextStates = listOf(PENDING_PROFILE, TESTING, READY)
        verify(expectedNextStates, sut)
    }

    @Test
    fun `valid next stages from READY`() {
        val sut = READY
        expectThat(sut.name).isEqualTo("Ready")
        val expectedNextStates = listOf(READY, RUNNING, PENDING_PROFILE)
        verify(expectedNextStates, sut)

    }

    @Test
    fun `valid next stages from TESTING`() {
        val sut = TESTING
        expectThat(sut.name).isEqualTo("Testing")
        val expectedNextStates = listOf(TESTING, PENDING_PROFILE)
        verify(expectedNextStates, sut)
    }

    @Test
    fun `valid next stages from RUNNING`() {
        val sut = RUNNING
        expectThat(sut.name).isEqualTo("Running")
        val expectedNextStates = listOf(PENDING_PROFILE, RUNNING, READY)
        verify(expectedNextStates, sut)
    }

    private fun verify(expectedNextStates: List<State>, stateUnderTest: State) {
        expectedNextStates.forEach {
            print("Testing can transition from $stateUnderTest to $it")
            expectThat(stateUnderTest.isValidNextState(it)).isTrue()
            println(" ... PASSED")
        }
        val disallowedStates = validStates() - expectedNextStates
        disallowedStates.forEach {
            print("Testing cannot transition from $stateUnderTest to $it")
            expectThat(stateUnderTest.isValidNextState(it)).isFalse()
            println(" ... PASSED")
        }
    }
}