package fermbot

import com.fasterxml.jackson.databind.ObjectMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * POJO that represents Brewfather customer stream. This value can be sent via POST to the batch for logging
 * Do not modify variable names in this class, you will break Breafather integration
 * @author Zachary Richards
 */
data class BrewfatherPayload(val name: String, val temp: Double, val gravity: Double) {
   val temp_unit = "F"
   val gravity_unit = "G"
}

@Singleton
class BrewfatherPayloadFactory @Inject constructor(private val configuration: Configuration) {

   fun createBrewfatherPayload(temp: Double, gravity: Double): BrewfatherPayload {
      return BrewfatherPayload(configuration.deviceName, temp, gravity)
   }
}
