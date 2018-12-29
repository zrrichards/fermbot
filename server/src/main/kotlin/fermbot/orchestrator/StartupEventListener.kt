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

import fermbot.Application
import fermbot.Configuration
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ShutdownEvent
import io.micronaut.discovery.event.ServiceStartedEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerShutdownEvent
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

    @Value("\${fermbot.banner-enabled:true}") //todo doc
    private var bannerEnabled = true

    @EventListener
    fun startupCompleted(event: ServiceStartedEvent) {
        if (bannerEnabled) {
            logger.info("""
                
  ______                  ____        _   
 |  ____|                |  _ \      | |  
 | |__ ___ _ __ _ __ ___ | |_) | ___ | |_ 
 |  __/ _ \ '__| '_ ` _ \|  _ < / _ \| __|
 | | |  __/ |  | | | | | | |_) | (_) | |_ 
 |_|  \___|_|  |_| |_| |_|____/ \___/ \__|
                                          
                                          
""")
        }
        logger.info("Started FermBot v0.1. Device name: " + configuration.deviceName)
        logger.info("""
Thank you for using the FermBot. This project is licenced under the GPLv3. 
You may use this software however you wish (even for commercial purposes). However, if you modify it, you
MUST release the code under the same license. Feel free to submit a Pull Request on Github!
Please report any issues to https://github.com/zrrichards/fermbot ... Happy fermenting!""".trimIndent())
    }
}
