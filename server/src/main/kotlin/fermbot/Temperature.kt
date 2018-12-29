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

        override fun toString(): String {
            return symbol
        }
    }

    fun get(unit: Unit): Double {
        return if (unit == this.unit) {
            value
        } else {
            this.unit.converter(value)
        }
    }

    /**
     * Returns temperature in the format of:
     * "64.8F", "43.0C" etc.
     */
    override fun toString() : String {
        return "$value${unit.symbol}"
    }

    fun asF(): Double {
        return get(Unit.FAHRENHEIT)
    }

    fun asC(): Double {
        return get(Unit.CELSIUS)
    }

    fun toStringF() : String {
        return "${asF()}F"
    }

    operator fun minus(other: Temperature): Temperature {

        //can use F or C it doesn't matter
        return (this.asF() - other.asF()).toF()
    }

    operator fun compareTo(other: Temperature): Int {
        return (asF() - other.asF()).sign.toInt()
    }
}

fun fromSymbol(symbol: String): Temperature.Unit {
    return when (symbol) {
        "C" -> Temperature.Unit.CELSIUS
        "F" -> Temperature.Unit.FAHRENHEIT
        else -> { throw IllegalArgumentException("Unrecognized temperature symbol '$symbol'") }
    }
}

fun Double.toF(): Temperature {
    return Temperature(this, Temperature.Unit.FAHRENHEIT)
}

fun Double.toC(): Temperature {
    return Temperature(this, Temperature.Unit.CELSIUS)
}

@Singleton
class TemperatureSerializer : JsonSerializer<Temperature>() {
    override fun serialize(value: Temperature, gen: JsonGenerator, serializers: SerializerProvider) {

        //Serialize 68.5 degrees Fahrenheit as "68.5F"
        gen.writeString("${value.value}${value.unit.symbol}")
    }
}

@Singleton
class TemperatureDeserializer : JsonDeserializer<Temperature>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Temperature {
        val tempAsString = p.valueAsString
        val unit = fromSymbol(tempAsString.takeLast(1))
        val temp = tempAsString.take(tempAsString.lastIndex).toDouble()
        return Temperature(temp, unit)
    }
}

