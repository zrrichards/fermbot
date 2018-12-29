package fermbot.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A mechanism for persisting and appending to rolling fermentation shapshot data
 * @author Zachary Richards
 * @version 12/17/19
 */
interface FermentationSnapshotPersister {
    fun append(snapshot: FermentationSnapshot)
    fun readAll(): List<FermentationSnapshot>
    fun clear()
}

@Singleton
class FileBasedFermentationSnapshotPersister @Inject constructor(private val objectMapper: ObjectMapper): FermentationSnapshotPersister {
    override fun clear() {
        file.delete()
    }

    private val file = Paths.get(".fermentation-snapshots").toFile()

    override fun append(snapshot: FermentationSnapshot) {
        file.appendText(objectMapper.writeValueAsString(snapshot) + "\n")
    }

    override fun readAll(): List<FermentationSnapshot> {
        return file.readLines().map { objectMapper.readValue<FermentationSnapshot>(it) }.toList()
    }


    //todo function to determine if SG is unchanged for a certain amount of time
    //todo ensure using a cache or circular buffer in memory so list of snapshots doesn't grow too much (that would be in controller).
}