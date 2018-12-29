package fermbot

import fermbot.hardwarebridge.tempcontrol.ActiveHighDigitalOutputDevice
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.HeatingMode
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 *
 * @author Zachary Richards
 * @version 12/20/19
 */
class TemperatureActuatorSpec {

    @Test
    fun `can create controller and read its current state`() {
        val mockHeater = spyDevice()
        val mockCooler = spyDevice()
        val controller = TemperatureActuator(mockHeater, mockCooler)
        expectThat(controller.currentMode).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can enable heating`() {
        val mockHeater = spyDevice()
        val mockCooler = spyDevice()
        val controller = TemperatureActuator(mockHeater, mockCooler)

        controller.setHeatingMode(HeatingMode.HEATING)

        verify(exactly = 1) {
            mockHeater.enable()
            mockCooler.disable()
        }

        verify(exactly = 0) {
            mockHeater.disable()
            mockCooler.enable()
        }
    }
}

class InMemoryActiveHighDigitalOutputDevice : ActiveHighDigitalOutputDevice {

    private var isEnabled = false

    override fun isEnabled() = isEnabled

    override fun enable() {
        isEnabled = true
    }

    override fun disable() {
        isEnabled = false
    }
}

private fun spyDevice(): InMemoryActiveHighDigitalOutputDevice {
    return spyk(InMemoryActiveHighDigitalOutputDevice())
}