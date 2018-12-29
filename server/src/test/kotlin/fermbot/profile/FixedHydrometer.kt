package fermbot.profile

import fermbot.Hydrometer
import java.time.Instant

data class FixedHydrometer(override val specificGravity: Double, override val timestamp: Instant = Instant.now()) : Hydrometer

