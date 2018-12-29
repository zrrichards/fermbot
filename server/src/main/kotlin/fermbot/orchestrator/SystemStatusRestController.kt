package fermbot.orchestrator
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

import fermbot.orchestrator.SystemStatistics
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version $ 12/8/19
 */
@Controller("/status")
class SystemStatusRestController {

    @Inject
    private lateinit var systemStatistics: SystemStatistics

    @Get("/")
    fun getStatus() : SystemStatistics {
       return systemStatistics
    }
}