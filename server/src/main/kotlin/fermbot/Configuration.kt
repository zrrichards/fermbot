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

import io.micronaut.context.annotation.Value
import java.time.Instant
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Singleton
class Configuration {

    @Value("\${micronaut.application.name}")
    private lateinit var namePrefix: String

    @Value("\${fermbot.suffix}")
    private lateinit var nameSuffix: String

    val deviceName: String by lazy {
        namePrefix + "_" + nameSuffix
    }

    val startupTime = Instant.now()

    @Value("\${brewfather.custom-stream-id}")
    lateinit var brewfatherCustomStreamId: String

    @Value("\${fermbot.pytilt-script-path}")
    lateinit var pytiltScriptPath: String
}