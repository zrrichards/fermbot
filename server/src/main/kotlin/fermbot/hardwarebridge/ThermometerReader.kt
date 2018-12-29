package fermbot.hardwarebridge

import fermbot.Thermometer

/**
 *
 * @author Zachary Richards
 * @version 12/16/19
 */
interface ThermometerReader {

    fun getDevices(): Thermometer
}