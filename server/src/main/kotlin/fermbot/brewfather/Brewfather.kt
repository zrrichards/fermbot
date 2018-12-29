package fermbot.brewfather

import fermbot.Temperature
import java.util.*

interface Brewfather {

    fun updateBatchDetails(currentTemp: Optional<Temperature>, specificGravity: Optional<Double>): BrewfatherUploadResult
}
