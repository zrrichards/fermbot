package fermbot.hardwarebridge

interface TiltReader {

    /**
     * Read the attached Tilt and return it. The returned tilt is only a snapshot
     * at the given instant at which it was read.
     */
    fun readTilt(): Tilt
}
