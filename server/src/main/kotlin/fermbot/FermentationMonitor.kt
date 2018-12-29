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

import fermbot.brewfather.Brewfather
import fermbot.orchestrator.SystemStatistics
import io.micronaut.scheduling.annotation.Scheduled
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls tilt parsing and logs data to brewfather
 * @author Zachary Richards
 * @version $ 12/5/19
 */
@Singleton
class FermentationMonitor @Inject constructor(private val configuration: Configuration) {
    @Inject private lateinit var brewfather: Brewfather

    @Inject private lateinit var tiltReader: TiltReader

    @Inject private lateinit var systemStatistics: SystemStatistics


    @Scheduled(fixedRate = "905s", initialDelay = "10s") //905s = 15 min + 5 seconds (Brewfather max logging time is every 15 min)
    fun execute() {
        val tilt = tiltReader.readTilt()
        systemStatistics.latestTiltReading = tilt
        brewfather.updateBatchDetails(tilt.currentTemp, tilt.specificGravity)
    }
}