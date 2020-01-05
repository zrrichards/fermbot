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
        persister = FileBasedFermentationSnapshotPersister(objectMapper)
        persister.clear()
    }

    @AfterEach
    fun deleteFile() {
        persister.clear()
    }

    @Test
    fun `can append snapshot to file`() {
        val snapshot = FermentationSnapshot(temp = 48.0.toF(),currentSg=1.056, heatingMode = HeatingMode.OFF,
                currentSetpointIndex=0, setpoint=49.5.toF(), description = "foo")
        persister.append(snapshot)
        expectThat(persister.readAll()).isEqualTo(listOf(snapshot))
    }
}