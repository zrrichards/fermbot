package fermbot.hardwarebridge

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
interface DigitalOutput {
    fun setHigh()
    fun setLow()
    fun isHigh(): Boolean
    fun isLow() = !isHigh()
}