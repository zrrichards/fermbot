package fermbot.hardwarebridge

import strikt.api.expectThat
import strikt.assertions.isFalse

/**
 *
 * @author Zachary Richards
 * @version 1/7/20
 */
class NullThermometerReaderSpec {
    fun `a null thermometer reader always returns an empty optional`() {
        val result = NullThermometerReader().getDevices()
        expectThat(result.isPresent).isFalse()
    }
}