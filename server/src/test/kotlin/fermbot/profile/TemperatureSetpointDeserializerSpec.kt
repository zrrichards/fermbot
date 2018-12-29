package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fermbot.toF
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 12/17/19
 */
@MicronautTest
class TemperatureSetpointDeserializerSpec {

    @Inject
    lateinit var objectMapper: ObjectMapper


    @Test
    fun `can deserialize specific-gravity based setpoint`() {
        val json= """    {
                "tempSetpoint": "48F",
                "untilSg": 1.023,
                "stageDescription": "Primary"
            },
            """

        val deser = json.deserialize()
        expectThat(deser).isEqualTo(SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary"))
    }

    @Test
    fun `can deserialize specific-gravity based setpoint without a description`() {
        val json= """    {
                "tempSetpoint": "48F",
                "untilSg": 1.023
            },
            """

        val deser = json.deserialize()
        expectThat(deser).isEqualTo(SpecificGravityBasedSetpoint(48.0.toF(), 1.023, ""))
    }

    @Test
    fun `can deserialize time-based setpoint`() {
        val json = """{
            "tempSetpoint": "62F",
            "duration": "P2D",
            "stageDescription": "Diacetyl Rest",
            "includeRamp": true
        }"""

        val deser = json.deserialize()
        expectThat(deser).isEqualTo(TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "Diacetyl Rest", true))
    }

    @Test
    fun `can deserialize time-based setpoint without a description`() {
        val json = """{
            "tempSetpoint": "62F",
            "duration": "P2D",
            "includeRamp": true
        }"""

        val deser = json.deserialize()
        expectThat(deser).isEqualTo(TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "", true))
    }

    private fun String.deserialize(): TemperatureSetpoint {
        return objectMapper.readValue(this)
    }
}

