package fermbot.profile

import fermbot.cascadeOptionals
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.FermentationMonitorTask
import fermbot.monitor.HeatingMode
import org.slf4j.LoggerFactory

class TemperatureControlTask(private val setpointDeterminer: SetpointDeterminer, private val hydrometerReader: ThermoHydrometerReader, private val hysteresisProfile: HysteresisProfile, private val thermometerReader: ThermometerReader, private val temperatureActuator: TemperatureActuator, private val fermentationMonitorTask: FermentationMonitorTask) : Runnable {

    private val logger = LoggerFactory.getLogger(TemperatureControlTask::class.java)

    override fun run() {
        try {
            run0()
        } catch (e: Exception) {
            logger.error("Caught exception running temperature control task:", e)
        }
    }

    private fun run0() {
        val thermohydrometer = hydrometerReader.readTilt()
        val setpoint = setpointDeterminer.getSetpoint(thermohydrometer)
        val currentHeatingMode = temperatureActuator.currentHeatingMode
        val thermometer = thermometerReader.getDevices()

        // if the tilt and ds18b20 are both present, use the ds18b20, otherwise use the tilt
        val bestThermometer = cascadeOptionals(thermohydrometer, thermometer)

        val currentTempString = bestThermometer.map { it.currentTemp.toStringF() }.orElse("None")

        val desiredHeatingMode = if (bestThermometer.isPresent) {
            hysteresisProfile.determineHeatingMode(setpoint.temperature, bestThermometer.get(), currentHeatingMode)
        } else {
            logger.warn("Thermometer not present. Heating mode being set to off")
            HeatingMode.OFF
        }
        if (currentHeatingMode != desiredHeatingMode) {
            logger.info("Current Setpoint: $setpoint. Current Temperature: $currentTempString")
            temperatureActuator.setHeatingMode(desiredHeatingMode)
            fermentationMonitorTask.run() // if we change heating modes, we need to capture it
        }
    }
}