package fermbot.profile

import fermbot.toF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/13/19
 */
class SetpointDeterminerSpec {

    val gravityBasedLagerProfile = listOf(
        SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary"),
        TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "Diacetyl Rest", false),
        TimeBasedSetpoint(34.0.toF(), Duration.ofDays(14), "Cold Crash", true)
    )

    class SetpointCompletionPersister : Persister<SetpointCompletion> {
        override fun hasPersistedData() = false
        override fun read() = throw UnsupportedOperationException()
        override fun persist(currentProfile: SetpointCompletion) { /* do nothing */ }
        override fun clear() { /* do nothing */ }
    }

    @Test
    fun `the initial profile stage is the first profile by default`() {
        val profileController = SetpointDeterminer(gravityBasedLagerProfile, SetpointCompletionPersister(), )
        expectThat(profileController.currentSetpointIndex).isEqualTo(0)
    }

    @Test
    fun `initial setpoint is not changed if gravity is too high`() {
        val profileController = SetpointDeterminer(gravityBasedLagerProfile, SetpointCompletionPersister(), )
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.040).toOptional())
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[0])
    }

    @Test
    fun `initial setpoint is changed if gravity is equal to what is defined in the profile`() {
        val profileController = SetpointDeterminer(gravityBasedLagerProfile, SetpointCompletionPersister(), )
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.023).toOptional())
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[1])
        expectThat(profileController.currentSetpointIndex).isEqualTo(1)
    }

    @Test
    fun `initial setpoint is changed if gravity is less than what is defined in the profile`() {
        val profileController = SetpointDeterminer(gravityBasedLagerProfile, SetpointCompletionPersister(), )
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.020).toOptional())
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[1])
        expectThat(profileController.currentSetpointIndex).isEqualTo(1)
    }

    //todo all time based setpoints
    @Test
    fun `time-based setpoint is not fulfilled if not enough time has passed`() {
        val setpoints = listOf(
                TimeBasedSetpoint(48.0.toF(), Duration.ofDays(2), "", false)
        )
        val profileController = SetpointDeterminer(setpoints, SetpointCompletionPersister(), )
        expectThat(profileController.getSetpoint(NullHydrometer.toOptional())).isEqualTo(setpoints[0])
    }
}

fun <T : Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)
