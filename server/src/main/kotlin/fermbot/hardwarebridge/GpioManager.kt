package fermbot.hardwarebridge

/**
 * A wrapper around the Raspberry Pi GPIO devices to facilitate testing and abstraction
 * @author Zachary Richards
 * @version 12/12/19
 */
interface GpioManager {

    /**
     * Provisions the digital with the given name and on the given pin. The
     * default logic value is High
     */
    fun provisionActiveLowDigitalOutput(pinName: String, name: String): DigitalOutput


    /**
     * shut down the gpio system
     */
    fun shutdown()
}
