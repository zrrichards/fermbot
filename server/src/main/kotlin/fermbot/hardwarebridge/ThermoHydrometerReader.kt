package fermbot.hardwarebridge

import java.util.*

interface ThermoHydrometerReader {

    /**
     * Read the attached Tilt and return it. The returned tilt is only a snapshot
     * at the given instant at which it was read.
     */
    fun readTilt(): Optional<ThermoHydrometer>
}
