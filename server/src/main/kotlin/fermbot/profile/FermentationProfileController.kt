package fermbot.profile

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import fermbot.Temperature
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.orchestrator.toPrettyString
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
@Controller("/profile")
class FermentationProfileRestController @Inject constructor(private val profilePersister: Persister<List<TemperatureSetpoint>>, private val temperatureActuator: TemperatureActuator) {

    private val currentProfile: MutableList<TemperatureSetpoint> = if (profilePersister.hasPersistedData()) {
        profilePersister.read().toMutableList()
    } else {
        mutableListOf()
    }

    private val logger = LoggerFactory.getLogger(FermentationProfileRestController::class.java)

    private var setpointDeterminer: SetpointDeterminer

    init {
        logger.info("CurrentProfile: {}", currentProfile)
        setpointDeterminer = SetpointDeterminer(currentProfile) //TODO this is hardcoding that fermentation started now
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

    fun clearProfile() {
        currentProfile.clear()
    }

    fun getCurrentHeatingMode() = temperatureActuator.getCurrentHeatingMode()//FIXME I don't really like reaching through the rest controller
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