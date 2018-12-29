package fermbot
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
