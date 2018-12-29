package fermbot.orchestrator

import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.HeatingMode
import fermbot.profile.TemperatureControllerTestPayload
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.inject.Inject

/**
 * A controller that allows the user to verify that all hardware connections have been made correctly.
 * This class cycles through the hardware options on the system and allows the user to verify that things are working
 * properly
 * @author Zachary Richards
 * @version 12/22/19
 */
@Controller("/test")
class HardwareTester @Inject constructor(private val temperatureActuator: TemperatureActuator, private val thermometerManager: ThermometerReader) {

    private val logger = LoggerFactory.getLogger(HardwareTester::class.java)

    /**
     * Allows the user for force activate the temperature controller for the given amount of time.
     * This is useful for testing the circuit wiring and ensuring the relays are working correctly
     */
    @Post("/heating-mode")
    fun testTemperatureControl(@Body test: TemperatureControllerTestPayload) {
        val initialHeatingMode = temperatureActuator.currentHeatingMode
        val duration = test.duration.coerceAtMost(Duration.ofSeconds(30))
        if (duration != test.duration) {
            logger.warn("Ignoring given duration value of $duration. Using maximum value of 30 seconds")
        }
        logger.info("Testing mode: ${test.mode} for $duration")
        temperatureActuator.setHeatingMode(test.mode)
        Thread.sleep(duration.toMillis())
        logger.info("Test over. Returning heating mode to previous state of: {}", initialHeatingMode)
        temperatureActuator.setHeatingMode(initialHeatingMode)
    }

    @Post("/full-hardware")
    fun testFullHardware(@Body heatingTestPayload: HeatingTestPayload) {
        val stepDuration = heatingTestPayload.stepDuration
        val reps = HeatingMode.values().size * 3 //3 heating modes 3 times
        logger.info("\n\n\n===== Starting hardware test. Repeating $reps times. Step duration: ${stepDuration.seconds} seconds =====")
        repeat(reps) { currentRep ->
            val modeToTest = getHeatingModeToTest(currentRep)
            logger.info("Activating heating mode: $modeToTest")

            if (modeToTest != HeatingMode.OFF) {
                logger.info("\tCorresponding device should be on (and other device should be off)")
            } else {
                logger.info("\tBoth devices should be off")
            }

            temperatureActuator.setHeatingMode(modeToTest)
            val thermometer = thermometerManager.getDevices()
            thermometer.ifPresent {
                logger.info("Thermometer (id=${it.id}) is reading: ${it.currentTemp.asF()}F")
            }
            logger.info("End of iteration ${currentRep + 1}/$reps. Pausing for $stepDuration\n")
            Thread.sleep(stepDuration.toMillis())
        }
        logger.info("===== Hardware Test complete =====\n\n\n")
    }

    private fun getHeatingModeToTest(currentRep: Int): HeatingMode {

        //FIXME ignore modes that aren't configured
        val index = currentRep.rem(HeatingMode.values().size)
        return HeatingMode.values()[index]
    }
}

data class HeatingTestPayload(val stepDuration: Duration)


