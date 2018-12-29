package fermbot.hardwarebridge.simulation

import fermbot.Thermometer
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.ThermometerReader
import fermbot.monitor.HeatingMode
import fermbot.profile.Environments
import fermbot.profile.FermbotProperties
import fermbot.profile.FermentationProfileController
import fermbot.toF
import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
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
@Requires(env=[Environments.SIMULATION], property= FermbotProperties.isDs18b20Enabled, value="true")
class SimulationDs18b20Manager : ThermometerReader {
    private val logger = LoggerFactory.getLogger(SimulationDs18b20Manager::class.java)

    var fermentationProfileController: FermentationProfileController? = null

    var prevTemp = fermentationProfileController?.currentSetpoint?.tempSetpoint ?: 50.0.toF()
    var prevHeatingMode = HeatingMode.OFF

    init {
        "Initializing: ${this.javaClass.simpleName}"
    }

    var thermometer: Optional<Thermometer> = Optional.of(DS18B20("simulation-id", 50.0.toF(), Instant.now()))

    override fun getDevices(): Optional<Thermometer> {
        if (thermometer.get().timestamp < Instant.now().minusMillis(6)) {
            refresh()
        }

        return thermometer
    }

    private fun refresh() {
        logger.debug("Invoking simulation DS18B20 Reader")
        val heatingMode = fermentationProfileController?.getCurrentHeatingMode() ?: HeatingMode.OFF
        if (heatingMode != prevHeatingMode) {
            logger.info("Noticing that heating mode changed from $prevHeatingMode to $heatingMode")
            prevHeatingMode = heatingMode
        }
        val changeInTemp = when(heatingMode) {
            HeatingMode.OFF -> Random.nextDouble(0.05)
            HeatingMode.HEATING -> 0.25
            HeatingMode.COOLING -> 0.097
        }

        //if temperature control is off generate some random wobble attributable to environmental conditions
        val sgn = when (heatingMode) {
            HeatingMode.OFF -> -1
            HeatingMode.HEATING ->  1
            HeatingMode.COOLING ->  -1
        }

        val nextTemp = prevTemp + (sgn * changeInTemp).toF()
        prevTemp = nextTemp
        thermometer = Optional.of(DS18B20("simulation-id", nextTemp, Instant.now()))
    }
}