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
import java.beans.ConstructorProperties

/**
 *
 * @author Zachary Richards
 * @version $ 12/5/19
 */
data class BrewfatherUploadResult @ConstructorProperties("result") constructor(val result: String) {
    fun isSuccessful(): Boolean {
        return result == "success"
    }

    fun isTooEarly(): Boolean {
        return result == TOO_EARLY
    }
}

const val TOO_EARLY = "Too Early"
