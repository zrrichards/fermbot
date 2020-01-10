package fermbot.hardwarebridge.tempcontrol

import io.micronaut.context.annotation.Bean
import javax.inject.Named
import javax.inject.Singleton

interface HeaterCoolerFactory {

    @Bean
    @Singleton
    @Named(value = "heater")
    fun createHeater(heaterPinName: String): DigitalOutputDevice

    @Bean
    @Singleton
    @Named(value = "cooler")
    fun createCooler(coolerPinName: String): DigitalOutputDevice
}
