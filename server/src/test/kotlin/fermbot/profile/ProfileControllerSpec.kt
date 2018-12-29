package fermbot.profile

import fermbot.toF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 *
 * @author Zachary Richards
 * @version 12/13/19
 */
class ProfileControllerSpec {

    val gravityBasedLagerProfile = listOf(
        SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary"),
        TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "Diacetyl Rest", false),
        TimeBasedSetpoint(34.0.toF(), Duration.ofDays(14), "Cold Crash", true)
    )

    @Test
    fun `the initial profile stage is the first profile by default`() {
        val profileController = ProfileController(gravityBasedLagerProfile)
        expectThat(profileController.getCurrentSetpointIndex()).isEqualTo(0)
    }

    @Test
    fun `initial setpoint is not changed if gravity is too high`() {
        val profileController = ProfileController(gravityBasedLagerProfile)
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.040))
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[0])
    }

    @Test
    fun `initial setpoint is changed if gravity is equal to what is defined in the profile`() {
        val profileController = ProfileController(gravityBasedLagerProfile)
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.023))
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[1])
        expectThat(profileController.getCurrentSetpointIndex()).isEqualTo(1)
    }

    @Test
    fun `initial setpoint is changed if gravity is less than what is defined in the profile`() {
        val profileController = ProfileController(gravityBasedLagerProfile)
        val setpoint = profileController.getSetpoint(FixedHydrometer(1.020))
        expectThat(setpoint).isEqualTo(gravityBasedLagerProfile[1])
        expectThat(profileController.getCurrentSetpointIndex()).isEqualTo(1)
    }

    //todo all time based setpoints
    @Test
    fun `time-based setpoint is not fulfilled if not enough time has passed`() {
        val setpoints = listOf(
                TimeBasedSetpoint(48.0.toF(), Duration.ofDays(2), "", false)
        )
        val profileController = ProfileController(setpoints,0, Instant.now().minus(1, ChronoUnit.DAYS))
        expectThat(profileController.getSetpoint(NullHydrometer)).isEqualTo(setpoints[0])
    }
}

