package fermbot.hardwarebridge

import fermbot.Temperature
import fermbot.Thermometer
import java.time.Instant

/**
 * A POJO model of a DS18B20 temperature sensor.
 * Each sensor has a unique ID defined at manufacture
 * This class represents a snapshot of the temperature at an instant, it is not updated.
 */
data class DS18B20(val id: String, override val currentTemp: Temperature, override val timestamp: Instant) : Thermometer
