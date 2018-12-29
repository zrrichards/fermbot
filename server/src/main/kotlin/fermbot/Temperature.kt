package fermbot

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class Temperature(private val value: Double, val unit: Unit) {
    enum class Unit(val converter: (Double) -> Double, val symbol: String) {
        CELSIUS({ a -> a * 1.8 + 32}, "C"),
        FAHRENHEIT({ a -> (a - 32) / 1.8}, "F");

        fun fromSymbol(symbol: String): Unit {
            return when (symbol) {
                "C" -> CELSIUS
                "F" -> FAHRENHEIT
                else -> { throw IllegalArgumentException("Unrecognized temperature symbol '$symbol'") }
            }
        }
    }

    fun get(unit: Unit): Double {
        return if (unit == this.unit) {
            value
        } else {
            this.unit.converter(value)
        }
    }
}

fun celsius(value: Double): Temperature {
    return Temperature(value, Temperature.Unit.CELSIUS)
}

fun celsius(value: Int): Temperature {
    return celsius(value.toDouble())
}

fun fahrenheit(value: Double): Temperature {
    return Temperature(value, Temperature.Unit.FAHRENHEIT)
}
