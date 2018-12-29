package fermbot.hardwarebridge

import fermbot.Thermometer
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/16/19
 */
interface ThermometerReader {

    fun getDevices(): Optional<Thermometer>
}