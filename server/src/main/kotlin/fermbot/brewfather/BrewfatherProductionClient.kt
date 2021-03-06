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

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.*
import fermbot.orchestrator.SystemStatistics
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.MediaType
import io.micronaut.http.client.DefaultHttpClient
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/5/19
 */
@Singleton
@Requires(property="fermbot.brewfather.enabled", value="true")
class BrewfatherProductionClient @Inject constructor(@Property(name= FermbotProperties.brewfatherCustomStreamId) private val brewfatherCustomStreamId: String, private val brewfatherPayloadFactory: BrewfatherPayloadFactory) : Brewfather {

    private val BREWFATHER_URL = "http://log.brewfather.net/stream?id=$brewfatherCustomStreamId"

    val client = DefaultHttpClient(URL(BREWFATHER_URL))

    val logger = LoggerFactory.getLogger(BrewfatherProductionClient::class.java)

    private val MAX_ATTEMPTS = 5
    private val SECONDS_BETWEEN_ATTEMPTS = 5

    @Inject private lateinit var systemStatistics: SystemStatistics

    init {
        logger.info("Initializaing Brewfather")
    }

    override fun updateBatchDetails(currentTemp: Optional<Temperature>, specificGravity: Optional<Double>, comment: String): BrewfatherUploadResult {
        require(currentTemp.isPresent || specificGravity.isPresent) {
            "CurrentTemp.isPresent? ${currentTemp.isPresent}, SpecificGravity.isPresent? ${specificGravity.isPresent}"
        }
        val payload = brewfatherPayloadFactory.createBrewfatherPayload(currentTemp, specificGravity, comment)
        var attempt = 0
        var lastException: Exception? = null

        do {
            try {
                return post0(payload)
            } catch (e: Exception) {
                logger.debug("Unable to upload to Brewfather. attempt $attempt of $MAX_ATTEMPTS", lastException)
                attempt++
                lastException = e
                Thread.sleep(SECONDS_BETWEEN_ATTEMPTS * 1000.toLong())
            }
        } while (attempt < MAX_ATTEMPTS)

        //unable to upload
        logger.error("Unable to upload to Brewfather after $MAX_ATTEMPTS attempts", lastException)
        throw lastException ?: IllegalStateException("lastException was null yet entered catch block... Please report this error") //shouldn't happen but just in case
    }


    private fun post0(payload: BrewfatherPayload): BrewfatherUploadResult {
        val result = client.exchange(POST(BREWFATHER_URL, payload).contentType(MediaType.APPLICATION_JSON), String::class.java).blockingFirst()
        val brewfatherResult = ObjectMapper().readValue(result.body().toString(), BrewfatherUploadResult::class.java)
        if (brewfatherResult.isSuccessful()) {
            systemStatistics.noteSuccessfulUpload()
            systemStatistics.lastSuccessfulUpload = Instant.now()
        } else {
            systemStatistics.noteFailedUpload()
        }
        return brewfatherResult
    }
}
