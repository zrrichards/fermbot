package fermbot

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import javax.inject.Singleton
import kotlin.math.sign

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class Temperature(val value: Double, val unit: Unit) {
    enum class Unit(val converter: (Double) -> Double, val symbol: String) {
        CELSIUS({ a -> a * 1.8 + 32}, "C"),
        FAHRENHEIT({ a -> (a - 32) / 1.8}, "F");

        override fun toString() = symbol
    }

    fun get(unit: Unit) = if (unit == this.unit) {
            value
        } else {
            this.unit.converter(value)
        }

    /**
     * Returns temperature in the format of:
     * "64.8F", "43.0C" etc.
     */
    override fun toString() =  "$value${unit.symbol}"

    fun asF() = get(Unit.FAHRENHEIT)

    fun asC() = get(Unit.CELSIUS)

    fun toStringF() = "${asF()}F"

    operator fun minus(other: Temperature) = (this.asF() - other.asF()).toF()

    operator fun compareTo(other: Temperature) = (asF() - other.asF()).sign.toInt()

    operator fun plus(other: Temperature) = (this.asF() + other.asF()).toF()
}

fun String.toTemperatureUnit() = when (this) {
        "C" -> Temperature.Unit.CELSIUS
        "F" -> Temperature.Unit.FAHRENHEIT
        else -> { throw IllegalArgumentException("Unrecognized temperature symbol '$this'") }
    }

fun Double.toF() = Temperature(this, Temperature.Unit.FAHRENHEIT)

fun Double.toC() = Temperature(this, Temperature.Unit.CELSIUS)

@Singleton
class TemperatureSerializer : JsonSerializer<Temperature>() {

    //Serialize 68.5 degrees Fahrenheit as "68.5F"
    override fun serialize(value: Temperature, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeString("${value.value}${value.unit.symbol}")
}

@Singleton
class TemperatureDeserializer : JsonDeserializer<Temperature>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Temperature {
        val tempAsString = p.valueAsString
        val unit = tempAsString.takeLast(1).toTemperatureUnit()
        val temp = tempAsString.take(tempAsString.lastIndex).toDouble()
        return Temperature(temp, unit)
    }
}

