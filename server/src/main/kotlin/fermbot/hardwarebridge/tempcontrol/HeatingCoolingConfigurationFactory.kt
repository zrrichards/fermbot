package fermbot.hardwarebridge.tempcontrol

import fermbot.profile.BeanDefinitions
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Factory
class HeatingCoolingConfigurationFactory {

    @Bean
    @Singleton
    fun determineHeatingCoolingConfiguration(@Named(BeanDefinitions.HEATER) heater: Optional<DigitalOutputDevice>, @Named(BeanDefinitions.COOLER) cooler: Optional<DigitalOutputDevice>): HeaterCoolerConfiguration {
        return if (heater.isPresent) {
            if (cooler.isPresent) {
                HeaterCoolerConfiguration.BOTH
            } else {
                HeaterCoolerConfiguration.HEATER
            }
        } else {
            if (cooler.isPresent) {
                HeaterCoolerConfiguration.COOLER
            } else {
                HeaterCoolerConfiguration.NONE
            }

        }
    }
}