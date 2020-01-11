package fermbot.hardwarebridge

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.toF
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 1/10/20
 */
@MicronautTest
class TiltDeserializationSpec {

    @Inject
    private lateinit var objectMapper : ObjectMapper

    @Test
    fun `can deserialize tilt`() {
        val json = """{"color": "BLACK", "specificGravity": 1.001, "temp": 56} """
        val actualTilt = objectMapper.readValue(json, Tilt::class.java)
        val expectedTilt = Tilt(TiltColors.BLACK, 1.001, 56.0)
        expectThat(actualTilt.currentTemp).isEqualTo(expectedTilt.currentTemp)
        expectThat(actualTilt.specificGravity).isEqualTo(expectedTilt.specificGravity)
    }
}