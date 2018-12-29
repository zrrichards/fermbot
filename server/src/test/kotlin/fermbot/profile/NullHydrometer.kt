package fermbot.profile

import fermbot.Hydrometer
import java.time.Instant

object NullHydrometer : Hydrometer {
    override val specificGravity: Double get() = throw RuntimeException("Cannot get specificGravity of Null Hydrometer")
    override val timestamp: Instant get() = throw RuntimeException("Cannot get timestamp of Null Hydrometer")
}
