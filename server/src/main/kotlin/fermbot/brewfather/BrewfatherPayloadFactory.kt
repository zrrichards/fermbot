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

import fermbot.Configuration
import fermbot.Temperature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrewfatherPayloadFactory @Inject constructor(private val configuration: Configuration) {

   fun createBrewfatherPayload(temp: Temperature, gravity: Double): BrewfatherPayload {
      return BrewfatherPayload(configuration.deviceName, temp.get(Temperature.Unit.FAHRENHEIT), gravity, Temperature.Unit.FAHRENHEIT.symbol)
   }
}

/**
 * POJO that represents Brewfather customer stream. This value can be sent via POST to the batch for logging
 * Do not modify variable names in this class, you will break Brewfather integration
 * @author Zachary Richards
 */
data class BrewfatherPayload(val name: String, val temp: Double, val gravity: Double, val temp_unit: String) {
   val gravity_unit = "G"
}
