package fermbot.hardwarebridge.simulation

import fermbot.hardwarebridge.DigitalOutput
import fermbot.hardwarebridge.GpioManager
import io.micronaut.context.annotation.Requires
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@Singleton
@Requires(notEnv=["Raspberry-Pi"])
class SimulationGpioManager : GpioManager {
    override fun provisionDigitalOutputDevice(pinName: String, name: String): DigitalOutput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}