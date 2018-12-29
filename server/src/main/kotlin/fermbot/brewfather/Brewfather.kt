package fermbot.brewfather

import fermbot.Temperature

interface Brewfather {

    fun updateBatchDetails(currentTemp: Temperature, specificGravity: Double): BrewfatherUploadResult
}
