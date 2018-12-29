package fermbot

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantISO8601Serializer: JsonSerializer<Instant>() {
    override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }

}
