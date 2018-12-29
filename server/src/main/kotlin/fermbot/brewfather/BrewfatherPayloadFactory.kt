package fermbot.brewfather
/*  Fermbot - Open source fermentation monitoring software.
 *  Copyright (C) 2019 Zachary Richards
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import com.fasterxml.jackson.annotation.JsonInclude
import fermbot.Temperature
import java.util.*
import javax.inject.Singleton

@Singleton
class BrewfatherPayloadFactory {

   //TODO store device name in bean
   fun createBrewfatherPayload(temp: Optional<Temperature>, gravity: Optional<Double>, comment: String): BrewfatherPayload {
      return BrewfatherPayload(name = "test",
                               temp = temp.ifPresentOrNull { it.value },
                               temp_unit = temp.ifPresentOrNull { it.unit.symbol },
                               gravity = gravity.getOrNull(),
                               gravity_unit = "G",
                               comment = if (comment.isBlank()) { null } else { comment })
   }
}



/**
 * POJO that represents Brewfather customer stream. This value can be sent via POST to the batch for logging
 * Do not modify variable names in this class, you will break Brewfather integration. We are depending on Jackson's
 * default serialization to serialize this object properly
 * @author Zachary Richards
 *
 * Format from the Brewfather Documentation. Every value included for completeness but most aren't supported in
 * this version of FermBot
 *
 * {
 * "name": "YourDeviceName",
 * "temp": 20.32,
 * "aux_temp": 15.61, // Fridge Temp
 * "ext_temp": 6.51, // Room Temp
 * "temp_unit": "C", // C, F, K
 * "gravity": 1.042,
 * "gravity_unit": "G", // G, P
 * "pressure": 10,
 * "pressure_unit": "PSI", // PSI, BAR, KPA
 * "ph": 4.12,
 * "comment": "Hello World",
 * "beer": "Pale Ale"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BrewfatherPayload(val name: String,
                             val temp: Double? = null,
                             val aux_temp: Double? = null,
                             val ext_temp: Double? = null,
                             val temp_unit: String? = null,
                             val gravity: Double? = null,
                             val gravity_unit: String? = null,
                             val pressure: Double? = null,
                             val pressure_unit: String? = null,
                             val comment: String? = null,
                             val beer: String? = null)  {
   init {
      check(temp_unit in listOf("C", "F", "K"))
      check(gravity_unit in listOf("G", "P"))
      check(pressure_unit in listOf("PSI", "BAR", "KPA"))
      checkBothOrNeither((temp != null || aux_temp != null || ext_temp != null), temp_unit == null) //if at least one temp is given, then we need a unit
      checkBothOrNeither(gravity == null, gravity_unit == null)
   }
}

/**
 * Check whether both booleans are true or both are false. If one is true and one is false, an IllegalArgumentException will be thrown
 */
private fun checkBothOrNeither(b1: Boolean, b2: Boolean, message: () -> String = {""}) {
   if (b1 != b2) {
      throw IllegalArgumentException(message())
   }
}

/**
 * If the given optional is present, invoke the given function with the value from the optional and return the result,
 * otherwise return null
 */
private fun <T, U> Optional<T>.ifPresentOrNull(function: (T) -> U): U? {
   return if (isPresent) {
      function(this.get())
   } else {
      null
   }
}

/**
 * get the value of this optional or return null if the optional is empty
 */
private fun <T> Optional<T>.getOrNull(): T? {
    return ifPresentOrNull { it }
}
