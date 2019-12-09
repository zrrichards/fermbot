package fermbot

import io.micronaut.scheduling.annotation.Scheduled
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls tilt parsing and logs data to brewfather
 * @author Zachary Richards
 * @version $ 12/5/19
 */
@Singleton
class FermentationMonitor @Inject constructor(private val configuration: Configuration) {
    private val logger = LoggerFactory.getLogger(FermentationMonitor::class.java)!!

    @Inject private lateinit var brewfather: Brewfather

    @Inject private lateinit var tiltReader: TiltReader

    @Scheduled(fixedRate = "905s", initialDelay = "10s") //905s = 15 min + 5 seconds (Brewfather max logging time is every 15 min)
    fun execute() {
        val tilt = tiltReader.readTilt()
        logger.info(tilt.toString())
        brewfather.updateBatchDetails(tilt.currentTemp, tilt.specificGravity)
    }
}