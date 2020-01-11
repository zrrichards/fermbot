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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fermbot.InstantISO8601Serializer
import fermbot.hardwarebridge.ThermoHydrometer
import io.micronaut.context.annotation.Context
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Context
class SystemStatistics {

    var latestTiltReading: ThermoHydrometer? = null

    var successfulUploads = 0
        private set

    var failedUploads = 0
        private set

    var totalUploads: Int
        get() = successfulUploads + failedUploads
        set(_) { throw UnsupportedOperationException() }

    var successfulUploadPercentage: Double
        get() = successfulUploads.toDouble() / totalUploads * 100
        set(_) { throw UnsupportedOperationException() }

    @JsonSerialize(using= InstantISO8601Serializer::class, `as`=String::class)
    var lastUploadTime: Instant? = null

    private val startedAt = Instant.now()

    fun getUptime() = Duration.between(startedAt, Instant.now())!!

    fun noteSuccessfulUpload() {
        successfulUploads++
    }

    fun noteFailedUpload() {
        failedUploads++
    }

    override fun toString(): String {
        return "SystemStatistics(successfulUploads=$successfulUploads, failedUploads=$failedUploads, lastUploadTime=$lastUploadTime, uptime=${getUptime().toPrettyString()}, latestTiltReading=$latestTiltReading)"
    }
}

fun Duration.toPrettyString(): String {
    return minusNanos(nano.toLong()).toString().substring(2).replace("(\\d[HMS])(?!$)".toRegex(), "$1 ").toLowerCase()
}

