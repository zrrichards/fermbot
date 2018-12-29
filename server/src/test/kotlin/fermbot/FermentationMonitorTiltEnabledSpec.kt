package fermbot

import fermbot.brewfather.Brewfather
import fermbot.brewfather.BrewfatherProductionClient
import fermbot.brewfather.BrewfatherUploadResult
import fermbot.hardwarebridge.*
import fermbot.hardwarebridge.raspberrypi.RaspberryPiTiltReader
import fermbot.hardwarebridge.simulation.SimulationDs18b20Manager
import fermbot.monitor.FermentationMonitorTask
import fermbot.profile.toOptional
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/16/19
 */


@Property(name="fermbot.tilt.enabled", value="true")
@MicronautTest
class FermentationMonitorTiltEnabledSpec {

    @Inject
    private lateinit var monitor: FermentationMonitorTask

    @Inject
    private lateinit var brewfather: Brewfather

    @Singleton
    @Replaces(RaspberryPiTiltReader::class)
    class TiltReaderStub : ThermoHydrometerReader {
        override fun readTilt(): Optional<ThermoHydrometer> {
            return Optional.of(Tilt(TiltColors.BLACK, 1.052, 65.5))
        }
    }

    @Replaces(BrewfatherProductionClient::class)
    @Singleton
    class BrewfatherStub : Brewfather {

        var currentTemp: Temperature = Double.MIN_VALUE.toF()
        var specificGravity = Double.MIN_VALUE
        var comment = ""

        override fun updateBatchDetails(currentTemp: Optional<Temperature>, specificGravity: Optional<Double>, comment: String): BrewfatherUploadResult {
            currentTemp.ifPresent { this.currentTemp = it }
            specificGravity.ifPresent { this.specificGravity = it }
            this.comment = comment
            return BrewfatherUploadResult("success")
        }
    }

    @Replaces(NullThermometerReader::class)
    @Singleton
    class DS18B20Stub : ThermometerReader {
        override fun getDevices(): Optional<Thermometer> {
            return DS18B20(id = "foo-id",
                    currentTemp = 95.21.toF(),
                    timestamp = Instant.now()).toOptional()
        }
    }

    @Test
    fun `can take snapshot with tilt and DS18B20`() {
        monitor.run()
        with (brewfather as BrewfatherStub) {
            expectThat(currentTemp).isEqualTo(95.21.toF())
            expectThat(specificGravity).isEqualTo(1.052)
        }
    }
}
