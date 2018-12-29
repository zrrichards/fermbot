package fermbot.profile

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import fermbot.Temperature
import fermbot.Thermometer
import fermbot.cascadeOptionals
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.TaskScheduler
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
@Controller("/profile")
class FermentationProfileRestController @Inject constructor(private val profilePersister: Persister<List<TemperatureSetpoint>>, private val temperatureActuator: TemperatureActuator, private val hydrometerReader: ThermoHydrometerReader, private val hysteresisProfile: HysteresisProfile,
                                                            private val thermometerReader: ThermometerReader,
                                                            @param:Named(TaskExecutors.SCHEDULED) private val taskScheduler: TaskScheduler) {

    private val currentProfile: MutableList<TemperatureSetpoint> = if (profilePersister.hasPersistedData()) {
        profilePersister.read().toMutableList()
    } else {
        mutableListOf()
    }

    private val logger = LoggerFactory.getLogger(FermentationProfileRestController::class.java)

    private lateinit var setpointDeterminer: SetpointDeterminer

    init {
        logger.info("CurrentProfile: {}", currentProfile)
    }

    @Get("/")
    fun getProfile() : List<TemperatureSetpoint> {
        return currentProfile
    }

    @Post("/")
    fun setProfile(@Body stages: List<TemperatureSetpoint>) {
        with (currentProfile) {
            clear()
            addAll(stages)
        }
        logger.info("Fermentation profile changed to: {}", currentProfile)
        profilePersister.persist(currentProfile)
    }

    /**
     * Actually starts the fermentation controller and set point
     */
    @Post("/start")
    fun  start() {
        val fermentationStart = Instant.now()
        setpointDeterminer = SetpointDeterminer(currentProfile, 0, fermentationStart) //TODO this is hardcoding that fermentation started now
        logger.info("Starting fermentation profile")
        val temperatureControlTask = TemperatureControlTask(
                setpointDeterminer, hydrometerReader, hysteresisProfile, thermometerReader, temperatureActuator
        )
        temperatureControlTask.run() //schedule at fixed rate probably won't do this so we have to start it manually
        taskScheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofMinutes(10), temperatureControlTask)
    }

    fun clearProfile() {
        currentProfile.clear()
    }

    fun getCurrentHeatingMode() = temperatureActuator.getCurrentHeatingMode()//FIXME I don't really like reaching through the rest controller
}

class TemperatureControlTask(private val setpointDeterminer: SetpointDeterminer, private val hydrometerReader: ThermoHydrometerReader, private val hysteresisProfile: HysteresisProfile, private val thermometerReader: ThermometerReader, private val temperatureActuator: TemperatureActuator) : Runnable {

    private val logger = LoggerFactory.getLogger(TemperatureControlTask::class.java)

    override fun run() {
        val thermohydrometer = hydrometerReader.readTilt()
        val setpoint = setpointDeterminer.getSetpoint(thermohydrometer)
        val currentHeatingMode = temperatureActuator.getCurrentHeatingMode()
        val thermometer = thermometerReader.getDevices()

        // if the tilt and ds18b20 are both present, use the ds18b20, otherwise use the tilt
        val bestThermometer = cascadeOptionals(thermohydrometer, thermometer)

        val desiredHeatingMode = hysteresisProfile.determineHeatingMode(setpoint.tempSetpoint, bestThermometer)
        logger.info("Current Setpoint: $setpoint. Current Heating Mode: $currentHeatingMode. Changing Heating Mode to: $desiredHeatingMode.")
        temperatureActuator.setHeatingMode(desiredHeatingMode)
    }
}

@Singleton
class TemperatureSetpointDeserializer : JsonDeserializer<TemperatureSetpoint>() { //can't be an object due to Micronaut code generation
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TemperatureSetpoint {
        val node = p.codec.readTree<JsonNode>(p)
        val tempSetpointNode = node.get("tempSetpoint")
        val tempSetpoint = p.codec.treeToValue(tempSetpointNode, Temperature::class.java)
        val stageDescription = if (node.has("stageDescription")) {
                node.get("stageDescription").textValue()
            } else {
                ""
            }
        return if (node.has("untilSg")) {
                val untilSg = node.get("untilSg").doubleValue()
                SpecificGravityBasedSetpoint(tempSetpoint, untilSg, stageDescription)
            } else {
                val duration = p.codec.treeToValue(node.get("duration"), Duration::class.java)
                val includeRamp = node.get("includeRamp").booleanValue()
                TimeBasedSetpoint(tempSetpoint, duration, stageDescription, includeRamp)
            }
    }
}

@Singleton
class DurationSerializer : JsonSerializer<Duration>() {
    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }

}