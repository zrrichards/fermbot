package fermbot

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
