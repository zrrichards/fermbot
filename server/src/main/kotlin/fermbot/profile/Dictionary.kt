package fermbot.profile

object Environments {
    const val SIMULATION = "simulation"
    const val RASPBERRY_PI = "raspberrypi"
}

object BeanDefinitions {
    const val SETPOINT_COMPLETION_PERSISTER = "SetpointCompletionPersister"
    const val PROFILE_PERSISTER = "ProfilePersister"
    const val HEATER = "heater"
    const val COOLER = "cooler"
}

object FermbotProperties {
    const val isDs18b20Enabled = "fermbot.ds18b20.enabled"
    const val brewfatherCustomStreamId = "fermbot.brewfather.custom-stream-id"
    const val isTiltEnabled = "fermbot.tilt.enabled"
    const val isHeaterEnabled = "fermbot.heater.enabled"
    const val isCoolerEnabled = "fermbot.cooler.enabled"
    const val heaterPinName = "fermbot.heater.pin-name"
    const val coolerPinName = "fermbot.cooler.pin-name"

    const val simulationStep = "fermbot.simulation.step.duration"
}