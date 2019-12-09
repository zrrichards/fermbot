package fermbot

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version $ 12/9/19
 */
@Singleton
class SystemStatistics {

    @Inject private lateinit var configuration: Configuration

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

    var lastUploadTime: Instant? = null

    fun getUptime(): Duration {
        return Duration.between(configuration.startupTime, Instant.now())
    }

    fun noteSuccessfulUpload() {
        successfulUploads++
    }

    fun noteFailedUpload() {
        failedUploads++
    }

    override fun toString(): String {
        return "SystemStatistics(successfulUploads=$successfulUploads, failedUploads=$failedUploads, lastUploadTime=$lastUploadTime, uptime=${getUptime().toPrettyString()})"
    }
}

fun Duration.toPrettyString(): String {
    return minusNanos(nano.toLong()).toString().substring(2).replace("(\\d[HMS])(?!$)".toRegex(), "$1 ").toLowerCase()
}
