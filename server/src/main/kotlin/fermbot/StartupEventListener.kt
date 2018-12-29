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

import io.micronaut.discovery.event.ServiceStartedEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Singleton
class StartupEventListener {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    @Inject
    private lateinit var configuration: Configuration

    @EventListener
    fun startupCompleted(event: ServiceStartedEvent) {
        logger.info("Started Fermbot. Device name: " + configuration.deviceName)
    }
}
