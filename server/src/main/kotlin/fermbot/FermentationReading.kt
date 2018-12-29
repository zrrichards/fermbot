package fermbot

import java.time.Instant

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
data class FermentationReading(val timestamp: Instant, val specficGravity: Double, val temperature: Temperature)