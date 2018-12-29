package fermbot.hardwarebridge.simulation

import fermbot.Thermometer
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.ThermometerReader
import fermbot.monitor.HeatingMode
import fermbot.profile.FermentationProfileController
import fermbot.toF
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(SimulationDs18b20Manager::class.java)

    var fermentationProfileController: FermentationProfileController? = null

    var prevTemp = fermentationProfileController?.currentSetpoint?.tempSetpoint ?: 50.0.toF()

    init {
        "Initializing: ${this.javaClass.simpleName}"
    }
    override fun getDevices(): Optional<Thermometer> {
        logger.debug("Invoking simulation DS18B20 Reader")
        val heatingMode = fermentationProfileController?.getCurrentHeatingMode() ?: HeatingMode.OFF
        val changeInTemp = Random.nextDouble(0.1)

        //if temperature control is off generate some random wobble attributable to environmental conditions
        val sgn = when (heatingMode) {
            HeatingMode.OFF -> if (Random.nextBoolean()) { 1 } else { -1}
            HeatingMode.HEATING ->  1
            HeatingMode.COOLING ->  -1
        }

        val nextTemp = prevTemp + (sgn * changeInTemp).toF()
        prevTemp = nextTemp
        return Optional.of(DS18B20("simulation-id", nextTemp, Instant.now()))
    }
}