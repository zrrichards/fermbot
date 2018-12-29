package fermbot.profile

import fermbot.Temperature
import fermbot.Thermometer
import fermbot.fromSymbol
import fermbot.monitor.HeatingMode
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Comparator
import kotlin.math.sign

/**
 * Temperature values are injected as strings (can't figure out how to do it via Micronaut and automatically parsing the value
 */
@Singleton
class HysteresisProfile @Inject constructor(@Property(name="fermbot.hysteresis.lower") lowerThresholdString: String, @Property(name="fermbot.hysteresis.upper") upperThresholdString: String) {

    private val lowerThreshold: TemperatureWindow = fromString(lowerThresholdString)
    private val upperThreshold: TemperatureWindow = fromString(upperThresholdString)

    private val logger = LoggerFactory.getLogger(HysteresisProfile::class.java)

    init {
        require(lowerThreshold > ZERO) {
            "Lower temperature threshold must be positive. Was $lowerThreshold"
        }
        require(upperThreshold > ZERO) {
            "Upper temperature threshold must be positive. Was $upperThreshold"
        }

        logger.info("Initializing Hysteresis profile: lowerThreshold=$lowerThreshold upperThreshold=$upperThreshold")
    }

    fun determineHeatingMode(setpoint: Temperature, thermometer: Optional<Thermometer>): HeatingMode {
        require(thermometer.isPresent)

        val currentTemp = thermometer.get().currentTemp
        if (currentTemp > setpoint + upperThreshold) {
            return HeatingMode.COOLING
        }
        if (currentTemp < setpoint - lowerThreshold) {
            return HeatingMode.HEATING
        }
        return HeatingMode.OFF
    }
}

private operator fun Temperature.minus(window: TemperatureWindow): Temperature {
    return Temperature(this.value - window.get(this.unit), this.unit)
}

private operator fun Temperature.plus(window: TemperatureWindow): Temperature {
    return Temperature(this.value + window.get(this.unit), this.unit)
}

val ZERO = TemperatureWindow(0.0, Temperature.Unit.CELSIUS)

/**
 * This class represents a temperature window (i.e. a change in temperature). 1 degree F is equal to 1.8 degrees C
 */
class TemperatureWindow(val value: Double, val unit: Temperature.Unit): Comparable<TemperatureWindow> {
    fun get(desiredUnit: Temperature.Unit): Double {
        return when {
            this.unit == desiredUnit -> value
            this.unit == Temperature.Unit.FAHRENHEIT -> //desired must be celcius
                value / 1.8
            else -> value * 1.8
        }
    }

    /**
     * Prints out the value in the form of "1.0C" or "-1.6F"
     */
    override fun toString(): String {
        return "$value${unit.symbol}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemperatureWindow

        if (value != other.value) return false
        if (unit != other.unit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun compareTo(other: TemperatureWindow): Int {
        return (get(unit) - other.get(unit)).sign.toInt()
    }
}

/**
 * Parse a window in the format of "1.5C" or "-1.64F" to a TemperatureWindow
 */
fun fromString(string: String) : TemperatureWindow {
    require(string.last().toString() in listOf("F", "C"))
    val unit = fromSymbol(string.takeLast(1))
    val temp = string.take(string.lastIndex).toDouble()
    return TemperatureWindow(temp, unit)
}
