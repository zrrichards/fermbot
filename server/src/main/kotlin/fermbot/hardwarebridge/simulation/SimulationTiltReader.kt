package fermbot.hardwarebridge.simulation

import fermbot.hardwarebridge.ThermoHydrometer
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.Tilt
import fermbot.hardwarebridge.TiltColors
import fermbot.profile.Environments
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.inject.Singleton

@Singleton
@Requires(env=[Environments.SIMULATION], property= FermbotProperties.isTiltEnabled, value="true")
class SimulationTiltReader : ThermoHydrometerReader {

    private val logger = LoggerFactory.getLogger(SimulationTiltReader::class.java)

    private val og = 1.070
    private val fg = 1.020
    private var decayFactor = 0.01
    private var decayAmount = 1 - decayFactor

    private val refreshInterval: Duration = Duration.ofMillis(6)

    init {
        logger.info("Initializing SimulationTiltReader")
    }

    private var tilt: Optional<ThermoHydrometer> = createTilt(og)

    override fun readTilt(): Optional<ThermoHydrometer> {
        if (tilt.get().timestamp < Instant.now().minusMillis(refreshInterval.toMillis())) {
            refresh()
        }
        return tilt
    }

    private fun refresh() {
        val nextValue = (og - fg) * decayAmount + fg
        decayAmount *= decayAmount
        tilt = createTilt(nextValue)
    }

    private fun createTilt(nextValue: Double): Optional<ThermoHydrometer> = Optional.of(Tilt(color = TiltColors.BLACK, specificGravity = nextValue, temp = 45.2))
}