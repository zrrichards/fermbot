package fermbot.monitor

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/31/19
 */
class FermentationSnapshotQueue(private val persister: FermentationSnapshotPersister, private val pruneAfter: Duration = Duration.ofDays(1)) {
    private val queue = ArrayDeque<FermentationSnapshot>()
    private val logger = LoggerFactory.getLogger(FermentationSnapshotQueue::class.java)

    val oldest: FermentationSnapshot
        get() = queue.first

    val newest: FermentationSnapshot
        get() = queue.last

    fun add(snapshot: FermentationSnapshot) {
        queue.add(snapshot)
        persister.append(snapshot)
        prune()
    }

    private fun prune() {
        if (queue.isEmpty()) {
            return
        }
        var removed = 0
        while (queue.peek().timestamp < Instant.now().minus(pruneAfter)) {
            queue.remove()
            removed++
        }
        logger.debug("Removed $removed entries from fermentation snapshot queue")
    }

    fun getAll(): List<FermentationSnapshot> {
        prune()
        return queue.toList().sortedBy { it.timestamp }
    }

    fun clear() = queue.clear()
}