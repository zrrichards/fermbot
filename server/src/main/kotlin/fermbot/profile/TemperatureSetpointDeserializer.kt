package fermbot.profile

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import fermbot.Temperature
import java.time.Duration
import javax.inject.Singleton

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