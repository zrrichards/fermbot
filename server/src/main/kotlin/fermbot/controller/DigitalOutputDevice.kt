package fermbot.controller

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
interface DigitalOutputDevice {
    fun enable()
    fun disable()
    fun isEnabled(): Boolean
}