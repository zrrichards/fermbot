package fermbot

import fermbot.monitor.FermentationMonitor
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 12/16/19
 */
@Property(name="tilt-enabled", value="")
@MicronautTest
class FermentationMonitorNullTiltSpec {

    @Inject
    private lateinit var monitor: FermentationMonitor

    @Test
    fun `can take snapshot when tilt not registered`() {
        monitor.execute()
        //no exception thrown
    }
}
