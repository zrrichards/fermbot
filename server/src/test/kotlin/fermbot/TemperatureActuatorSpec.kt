package fermbot

import fermbot.hardwarebridge.tempcontrol.ActiveHighDigitalOutputDevice
import fermbot.hardwarebridge.tempcontrol.HardwareBackedTemperatureActuator
import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.monitor.HeatingMode
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

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
        val controller = HardwareBackedTemperatureActuator(mockHeater, mockCooler, HeaterCoolerConfiguration.BOTH)
        expectThat(controller.currentHeatingMode).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can enable heating`() {
        val mockHeater = spyDevice()
        val mockCooler = spyDevice()
        val controller = HardwareBackedTemperatureActuator(mockHeater, mockCooler, HeaterCoolerConfiguration.BOTH)

        controller.setHeatingMode(HeatingMode.HEATING)

        verify(exactly = 1) {
            mockHeater.get().enable()
            mockCooler.get().disable()
        }

        verify(exactly = 0) {
            mockHeater.get().disable()
            mockCooler.get().enable()
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

private fun spyDevice(): Optional<ActiveHighDigitalOutputDevice> {
    return Optional.of(spyk(InMemoryActiveHighDigitalOutputDevice()))
}

//notes on how to mock micronaut bean factory
//@Factory
//@Replaces(factory= HardwareBackedHeaterCoolerFactory::class)
//class TestFactory : HeaterCoolerFactory {
//
//    @Bean
//    @Singleton
//    @Named("heater")
//    override fun createHeater(): ActiveHighDigitalOutputDevice {
//        return mockk()
//    }
//
//    @Bean
//    @Singleton
//    @Named("cooler")
//    override fun createCooler(): ActiveHighDigitalOutputDevice {
//        return mockk()
//    }
//}
