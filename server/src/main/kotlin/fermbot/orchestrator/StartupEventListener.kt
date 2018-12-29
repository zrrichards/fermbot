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

import fermbot.DeviceInfo
import io.micronaut.context.annotation.Context
import io.micronaut.context.env.Environment
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Context
class StartupEventListener @Inject constructor(deviceInfo: DeviceInfo, environment: Environment) {

    private val logger = LoggerFactory.getLogger(StartupEventListener::class.java)

    private var deviceName = deviceInfo.deviceName

    init {
        logger.info("""
                                                __    
                                        _(\    |@@|
                                       (__/\__ \--/ __       .~~~~~.
 ______                  ____        _    \___|----|  |   __ i=====i
|  ____|                |  _ \      | |       \ FB /\ )_ / _\|ccccc|
| |__ ___ _ __ _ __ ___ | |_) | ___ | |       /\__/\ \__O (__| F B |
|  __/ _ \ '__| '_ ` _ \|  _ < / _ \| __|    (--/\--)    \__/|ccccc|
| | |  __/ |  | | | | | | |_) | (_) | |_     _)(  )(_        `-===-'
|_|  \___|_|  |_| |_| |_|____/ \___/ \__|   `---''---`
                                      
""")
        logger.info("Started FermBot v0.1. Device name: $deviceName")
        logger.info("""
Thank you for using the FermBot. This project is licenced under the GPLv3. 
You may use this software however you wish (even for commercial purposes). However, if you modify it, you
MUST release the code under the same license. Feel free to submit a Pull Request on Github!
Please report any issues to https://github.com/zrrichards/fermbot ... Happy fermenting!""".trimIndent())
        logger.info("===== FERMBOT CONFIGURATION =====")
        environment.getProperties("fermbot").forEach {
            logger.info("""${it.key}: ${it.value}""")
        }
        logger.info("=================================")
    }
}
