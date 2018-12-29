package fermbot

import fermbot.brewfather.Brewfather
import fermbot.brewfather.BrewfatherProductionClient
import fermbot.brewfather.BrewfatherUploadResult
import fermbot.hardwarebridge.ThermoHydrometer
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.Tilt
import fermbot.hardwarebridge.TiltColors
import fermbot.hardwarebridge.raspberrypi.RaspberryPiTiltReader
import fermbot.monitor.FermentationMonitor
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/16/19
 */


@Property(name="tilt-enabled", value="true")
@MicronautTest
class FermentationMonitorTiltEnabledSpec {

    @Inject
    private lateinit var monitor: FermentationMonitor

    @Inject
    private lateinit var brewfather: Brewfather

    @Singleton
    @Replaces(RaspberryPiTiltReader::class)
    class TiltReaderStub : ThermoHydrometerReader {
        override fun readTilt(): ThermoHydrometer {
            return Tilt(TiltColors.BLACK, 1.052, 65.5)
        }
    }

    @Replaces(BrewfatherProductionClient::class)
    @Singleton
    class BrewfatherStub : Brewfather {

        var currentTemp: Temperature = Double.MIN_VALUE.toF()
        var specificGravity = Double.MIN_VALUE

        override fun updateBatchDetails(currentTemp: Temperature, specificGravity: Double): BrewfatherUploadResult {
            this.currentTemp = currentTemp
            this.specificGravity = specificGravity
            return BrewfatherUploadResult("success")
        }
    }

    @Test
    fun `can take snapshot with tilt`() {
        monitor.execute()
        with (brewfather as BrewfatherStub) {
            expectThat(currentTemp).isEqualTo(65.5.toF())
            expectThat(specificGravity).isEqualTo(1.052)
        }
    }
}
