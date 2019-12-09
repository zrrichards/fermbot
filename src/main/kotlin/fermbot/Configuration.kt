package fermbot

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