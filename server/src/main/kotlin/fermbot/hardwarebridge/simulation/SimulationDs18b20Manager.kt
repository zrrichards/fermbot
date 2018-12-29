package fermbot.hardwarebridge.simulation

import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.DS18B20Manager
import fermbot.toF
import io.micronaut.context.annotation.Requires
import java.time.Instant
import javax.inject.Singleton
import kotlin.random.Random

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@Singleton
@Requires(notEnv=["Raspberry-Pi"])
class SimulationDs18b20Manager : DS18B20Manager {
    override fun getDevices(): DS18B20 {
        return DS18B20("simulation-id", (Random.nextInt(320, 1000) / 10.0).toF(), Instant.now())
    }
}