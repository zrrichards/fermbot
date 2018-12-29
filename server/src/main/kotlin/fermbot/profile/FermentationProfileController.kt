package fermbot.profile

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import fermbot.Temperature
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
class FermentationProfileRestController @Inject constructor(private val profilePersister: ProfilePersister) {

    private val currentProfile: MutableList<TemperatureSetpoint> = if (profilePersister.hasPersistedProfile()) {
        profilePersister.readProfile().toMutableList()
    } else {
        mutableListOf()
    }

    private val logger = LoggerFactory.getLogger(FermentationProfileRestController::class.java)

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
        profilePersister.persistProfile(currentProfile)
    }

    fun clearProfile() {
        currentProfile.clear()
    }
}

@Singleton
class TemperatureSetpointDeserializer : JsonDeserializer<TemperatureSetpoint>() { //can't be an object due to Micronaut code generation
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TemperatureSetpoint {
        val node = p.codec.readTree<JsonNode>(p)
        val tempSetpointNode = node.get("tempSetpoint")
        val tempSetpoint = p.codec.treeToValue(tempSetpointNode, Temperature::class.java)
        val stageDescription = node.get("stageDescription").textValue()
        return when (node.has("untilSg")) {
            true -> {
                val untilSg = node.get("untilSg").doubleValue()
                SpecificGravityBasedSetpoint(tempSetpoint, untilSg, stageDescription)
            }
            false -> {
                val duration = p.codec.treeToValue(node.get("duration"), Duration::class.java)
                val includeRamp = node.get("includeRamp").booleanValue()
                TimeBasedSetpoint(tempSetpoint, duration, stageDescription, includeRamp)
            }
        }
    }
}

@Singleton
class DurationSerializer : JsonSerializer<Duration>() {
    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }

}