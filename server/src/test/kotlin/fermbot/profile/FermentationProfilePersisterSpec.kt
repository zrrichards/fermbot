package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.monitor.FermentationSnapshot
import fermbot.monitor.FermentationSnapshotPersister
import fermbot.monitor.FileBasedFermentationSnapshotPersister
import fermbot.monitor.HeatingMode
import fermbot.toF
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 12/17/19
 */
@MicronautTest
class FermentationProfilePersisterSpec {

    lateinit var persister: FermentationSnapshotPersister
    lateinit var file: File

    @Inject
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun initFile() {
        file = Paths.get(".temp").toFile()
        check(!file.exists())
        file.createNewFile()
        persister = FileBasedFermentationSnapshotPersister(file, objectMapper)
    }

    @AfterEach
    fun deleteFile() {
        file.delete()
    }

    @Test
    fun `can append snapshot to file`() {
        val snapshot = FermentationSnapshot(temp = 48.0.toF(),currentSg=1.056, heatingMode = HeatingMode.OFF,
                currentSetpointIndex=0)
        persister.append(snapshot)
        expectThat(persister.readAll()).isEqualTo(listOf(snapshot))
    }
}