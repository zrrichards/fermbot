package fermbot

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