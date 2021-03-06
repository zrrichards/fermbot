package fermbot

import java.time.Instant

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

/**
 * @author Zachary Richards
 */
interface Thermometer {

    val currentTemp: Temperature

    /**
     * Represents when the value was read
     */
    val timestamp: Instant

    /**
     * the ID of this device (must be unique to this instance of the fermbot)
     */
    val id: String
}

