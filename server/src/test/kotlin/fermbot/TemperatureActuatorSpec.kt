package fermbot

import fermbot.hardwarebridge.tempcontrol.*
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import javax.inject.Named
import javax.inject.Singleton

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
        val controller = HardwareBackedTemperatureActuator(mockHeater, mockCooler)
        expectThat(controller.getCurrentHeatingMode()).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can enable heating`() {
        val mockHeater = spyDevice()
        val mockCooler = spyDevice()
        val controller = HardwareBackedTemperatureActuator(mockHeater, mockCooler)

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
