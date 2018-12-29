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

import io.micronaut.scheduling.annotation.Scheduled
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Singleton
class SystemStateReporter {

    private val logger = LoggerFactory.getLogger(SystemStateReporter::class.java)

    @Inject
    private lateinit var systemStatistics: SystemStatistics

    @Scheduled(fixedRate="1h", initialDelay="1m")
    fun reportOnSystemState() {
       logger.info("Uptime: ${systemStatistics.getUptime().toPrettyString()}\tSuccessful Brewfather uploads (${systemStatistics.successfulUploads}/${systemStatistics.totalUploads}) -- ${systemStatistics.successfulUploadPercentage}%")
    }
}