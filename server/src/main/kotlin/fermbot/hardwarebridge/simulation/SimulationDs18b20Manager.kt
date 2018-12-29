package fermbot.hardwarebridge.simulation

import fermbot.Thermometer
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.ThermometerReader
import fermbot.toF
import io.micronaut.context.annotation.Requires
import java.time.Instant
import java.util.*
import javax.inject.Singleton
import kotlin.random.Random

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@Singleton
@Requires(notEnv=["Raspberry-Pi"])
class SimulationDs18b20Manager : ThermometerReader {
    override fun getDevices(): Optional<Thermometer> {
        return Optional.of(DS18B20("simulation-id", (Random.nextInt(320, 1000) / 10.0).toF(), Instant.now()))
    }
}