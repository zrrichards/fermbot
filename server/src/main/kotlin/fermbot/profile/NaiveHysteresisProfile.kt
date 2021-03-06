package fermbot.profile

import fermbot.Temperature
import fermbot.Thermometer
import fermbot.hardwarebridge.tempcontrol.HeaterCoolerConfiguration
import fermbot.monitor.HeatingMode
import fermbot.toTemperatureUnit
import io.micronaut.context.annotation.Value
import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sign

interface HysteresisProfile {
    val lowerThreshold: TemperatureWindow
    val upperThreshold: TemperatureWindow

    fun determineHeatingMode(setpoint: Temperature, thermometer: Thermometer, currentHeatingMode: HeatingMode): HeatingMode
}

//@Singleton
class NaiveHysteresisProfile constructor(@Value("\${fermbot.hysteresis.lower:1F}") override val lowerThreshold: TemperatureWindow, @Value("\${fermbot.hysteresis.upper:1F}") override val upperThreshold: TemperatureWindow, private val heatingCoolingConfiguration: HeaterCoolerConfiguration) : HysteresisProfile {

    private val logger = LoggerFactory.getLogger(NaiveHysteresisProfile::class.java)

    val totalWindow = lowerThreshold + upperThreshold

    //TODO make these configurable but default values can't be used with @inject constructors
    val min: TemperatureWindow = lowerThreshold * 1.2
    val max: TemperatureWindow = upperThreshold * 1.2

    init {
        require (lowerThreshold != ZERO) { "Lower threshold cannot be zero" }
        require (upperThreshold != ZERO) { "Lower threshold cannot be zero" }
        require (max > upperThreshold) { "Max value [$max] must be > upper threshold [$upperThreshold]" }
        require (min > lowerThreshold) { "Min value [$min] must be > lower threshold [$lowerThreshold]" }

        logger.info("Initializing Hysteresis profile: lowerThreshold=$lowerThreshold upperThreshold=$upperThreshold, min=$min, max=$max")
    }


    override fun determineHeatingMode(setpoint: Temperature, thermometer: Thermometer, currentHeatingMode: HeatingMode): HeatingMode {

        //If we don't have a thermometer, there's nothing we can do to measure the temperature so don't do anything for temp control
//        if (!thermometer.isPresent) { //todo move Optional to caller. don't make this class check if thermometer is present
//            logger.warn("No thermometer present, heating mode is being set to Off regardless of setpoint")
//            return HeatingMode.OFF
//        }

        //todo this is a naive implementation
        val currentTemp = thermometer.currentTemp
        val desiredHeatingMode = when(getHysteresisStatus(setpoint, currentTemp)) {
            HysteresisStatus.MAX -> HeatingMode.COOLING
            HysteresisStatus.ABOVE ->  when (currentHeatingMode) {
                HeatingMode.HEATING -> HeatingMode.OFF
                else -> HeatingMode.COOLING
            }
            HysteresisStatus.WITHIN -> currentHeatingMode
            HysteresisStatus.BELOW -> when (currentHeatingMode) {
                HeatingMode.COOLING -> HeatingMode.OFF
                else -> HeatingMode.HEATING
            }
            HysteresisStatus.MIN -> HeatingMode.HEATING
        }

        return heatingCoolingConfiguration.normalizeHeatingMode(desiredHeatingMode)
    }

    fun getHysteresisStatus(setpoint: Temperature, currentTemperature: Temperature) : HysteresisStatus {
        return when {
            currentTemperature >= setpoint + max  -> HysteresisStatus.MAX
            currentTemperature >= setpoint + upperThreshold -> HysteresisStatus.ABOVE
            currentTemperature <= setpoint - min -> HysteresisStatus.MIN
            currentTemperature <= setpoint - lowerThreshold -> HysteresisStatus.BELOW
            else -> HysteresisStatus.WITHIN
        }
    }
}




enum class HysteresisStatus {
    MAX,
    ABOVE,
    WITHIN,
    BELOW,
    MIN
}

fun symmetricNaiveHysteresisProfile(window: TemperatureWindow, configuration: HeaterCoolerConfiguration) = NaiveHysteresisProfile(window, window, configuration)

operator fun Temperature.minus(window: TemperatureWindow): Temperature {
    return Temperature(this.value - window.get(this.unit), this.unit)
}

operator fun Temperature.plus(window: TemperatureWindow): Temperature {
    return Temperature(this.value + window.get(this.unit), this.unit)
}

val ZERO = TemperatureWindow(0.0, Temperature.Unit.CELSIUS)

/**
 * This class represents a temperature window (i.e. a change in temperature). 1 degree F is equal to 1.8 degrees C
 */
class TemperatureWindow(val value: Double, val unit: Temperature.Unit): Comparable<TemperatureWindow> {

    init {
        require (value >= 0) { "Cannot create a negative temperature window of $value$unit" }
    }

    fun get(desiredUnit: Temperature.Unit): Double {
        return when {
            this.unit == desiredUnit -> value
            this.unit == Temperature.Unit.FAHRENHEIT -> //desired must be celcius
                value / 1.8
            else -> value * 1.8
        }
    }

    /**
     * Prints out the value in the form of "1.0C"
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

    operator fun plus(other: TemperatureWindow): TemperatureWindow {
        return TemperatureWindow((get(unit) + other.get(unit)), unit)
    }

    operator fun times(d: Double): TemperatureWindow {
        return TemperatureWindow(get(unit) * d, unit)
    }

    operator fun plus(other: Temperature): Temperature {
        return other + this
    }
}

/**
 * Parse a window in the format of "1.5C" or "-1.64F" to a TemperatureWindow
 */
fun fromString(string: String) : TemperatureWindow {

    if (string == "0") { //special case because a zero temperature window is the same regardless of the unit
        return ZERO
    }

    val unit = string.takeLast(1).toTemperatureUnit()
    val temp = string.take(string.lastIndex).toDouble()
    return TemperatureWindow(temp, unit)
}

@Singleton
class TemperatureWindowTypeConverter : TypeConverter<String, TemperatureWindow> {
    override fun convert(source: String, targetType: Class<TemperatureWindow>, context: ConversionContext): Optional<TemperatureWindow> {

        //doc says to return empty if value cannot be parsed but let any exception bubble up for now
        return Optional.of(fromString(source))
    }
}
