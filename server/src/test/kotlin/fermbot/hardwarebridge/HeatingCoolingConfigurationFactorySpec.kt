package fermbot.hardwarebridge

import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.hardwarebridge.tempcontrol.HeatingCoolingConfigurationFactory
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 1/7/20
 */
class HeatingCoolingConfigurationFactorySpec {

    val factory = HeatingCoolingConfigurationFactory()

    @Test
    fun `if neither neither nor cooler is present then heating cooling config is none`() {
        val config = factory.determineHeatingCoolingConfiguration(Optional.empty(),Optional.empty())
        expectThat(config).isEqualTo(HeaterCoolerConfiguration.NONE)
    }

    @Test
    fun `if heater present but cooler not present then config is heater`() {
        val config = factory.determineHeatingCoolingConfiguration(Optional.of(mockk()),Optional.empty())
        expectThat(config).isEqualTo(HeaterCoolerConfiguration.HEATER)
    }

    @Test
    fun `if heater not present but cooler present then config is cooler`() {
        val config = factory.determineHeatingCoolingConfiguration(Optional.empty(), Optional.of(mockk()))
        expectThat(config).isEqualTo(HeaterCoolerConfiguration.COOLER)
    }

    @Test
    fun `if both present then config is both`() {
        val config = factory.determineHeatingCoolingConfiguration(Optional.of(mockk()),Optional.of(mockk()))
        expectThat(config).isEqualTo(HeaterCoolerConfiguration.BOTH)
    }
}