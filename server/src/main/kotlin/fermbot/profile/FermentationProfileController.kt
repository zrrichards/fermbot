package fermbot.profile

import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.simulation.SimulationDs18b20Manager
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.FermentationMonitorTask
import fermbot.monitor.HeatingMode
import io.micronaut.context.env.Environment
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.TaskScheduler
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ScheduledFuture
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs
import kotlin.math.log10

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
@Controller //TODO fix this god object
class FermentationProfileController @Inject constructor(@param:Named(BeanDefinitions.PROFILE_PERSISTER) private val profilePersister: Persister<List<TemperatureSetpoint>>,
                                                        private val temperatureActuator: TemperatureActuator,
                                                        private val hydrometerReader: ThermoHydrometerReader,
                                                        private val hysteresisProfile: HysteresisProfile,
                                                        private val thermometerReader: ThermometerReader,
                                                        @param:Named(TaskExecutors.SCHEDULED) private val taskScheduler: TaskScheduler,
                                                        private val fermentationMonitorTask: FermentationMonitorTask,
                                                        @param:Named(BeanDefinitions.SETPOINT_COMPLETION_PERSISTER) private val setpointCompletionPersister: Persister<SetpointCompletion>,
                                                        private val environment: Environment) {

    val statistics = temperatureActuator.statistics
    private var temperatureControlFuture: ScheduledFuture<*>? = null
    private var fermentationMonitorFuture: ScheduledFuture<*>? = null

    private val BREWFATHER_UPLOAD_PERIOD = Duration.ofSeconds(15 * 60 + 10) //15 minutes + a few seconds as a buffer

    val currentSetpoint: TemperatureSetpoint
        get() = setpointDeterminer.currentStage

    private val currentProfile = profilePersister.loadOrElse(mutableListOf()).toMutableList()

    private val logger = LoggerFactory.getLogger(FermentationProfileController::class.java)

    private lateinit var setpointDeterminer: SetpointDeterminer

    val currentSetpointIndex: Int
        get() = setpointDeterminer.currentSetpointIndex

    init {
        logger.info("CurrentProfile: {}", if (currentProfile.isEmpty()) { "[empty]" } else { "\n${prettyFormatCurrentProfile()}"})
        fermentationMonitorTask.run() //initial post to brewfather so device is visible
        if (thermometerReader is SimulationDs18b20Manager) { //if we're simulating temperature, pass this instance to the thermometer.
            thermometerReader.fermentationProfileController = this
        }
    }

    @Get("/profile")
    fun getProfile() : List<TemperatureSetpoint> {
        return currentProfile
    }

    @Get("/snapshots")
    fun getSnapshots() = fermentationMonitorTask.snapshots

    fun setProfile(setpoints: List<TemperatureSetpoint>) {
        require(setpoints.isNotEmpty()) { "Must pass at least one Temperature setpoint" }
        with (currentProfile) {
            clear()
            addAll(setpoints)
        }
        logger.info("Fermentation profile changed to: \n{}", prettyFormatCurrentProfile())
        profilePersister.persist(currentProfile)
        setpointCompletionPersister.clear()
    }

    private fun prettyFormatCurrentProfile(): String {

        fun padToWidth(i: Int, size: Int): String {
            val str = StringBuilder()
            repeat(size - i.numDigits()) {
                str.append(" ")
            }
            str.append(i.toString())
            return str.toString()
        }

        val profileAsString = StringBuilder()
        currentProfile.forEachIndexed { i, currentStage ->
            profileAsString.append("\t${padToWidth(i + 1, currentProfile.size.numDigits())}: $currentStage\n") //display as one-based
        }
        return profileAsString.toString()
    }


    @Get("/status")
    fun status() : Any {
        return if (::setpointDeterminer.isInitialized) {
            setpointDeterminer.getRemainingStageInfo()
        } else {
            "No status to report. Fermentation not running"
        }
    }

    /**
     * Actually starts the fermentation controller and set point
     */
    fun start() {
        setpointDeterminer = SetpointDeterminer(currentProfile, setpointCompletionPersister, fermentationMonitorTask)
        logger.info("Starting fermentation profile")
        val temperatureControlTask = TemperatureControlTask(
                setpointDeterminer, hydrometerReader, hysteresisProfile, thermometerReader, temperatureActuator, fermentationMonitorTask
        )
        temperatureControlTask.run()
        fermentationMonitorTask.fermentationProfileController = this
        fermentationMonitorTask.clearSnapshots()
        if (Environments.SIMULATION in environment.activeNames) {
            val duration = determineSimulationStepDuration()
            temperatureControlFuture = taskScheduler.scheduleAtFixedRate(duration, duration, temperatureControlTask)
            fermentationMonitorFuture = taskScheduler.scheduleAtFixedRate(duration, duration, fermentationMonitorTask)
        } else {
            temperatureControlFuture = taskScheduler.scheduleAtFixedRate(Duration.ofMinutes(10), Duration.ofMinutes(10), temperatureControlTask)
            fermentationMonitorFuture = taskScheduler.scheduleAtFixedRate(BREWFATHER_UPLOAD_PERIOD, BREWFATHER_UPLOAD_PERIOD, fermentationMonitorTask)
        }
    }

    private fun determineSimulationStepDuration(): Duration {
        //in simulation mode, a second is equivalent to a day. Equivalent to every 10 minutes
        return Duration.ofMillis(7)
    }

    fun clearProfile() {
        logger.info("Clearing fermentation profile")
        currentProfile.clear()
    }

    fun getCurrentHeatingMode() = temperatureActuator.currentHeatingMode //FIXME I don't really like reaching through the rest controller

    fun isProfileSet(): Boolean {
        return currentProfile.isNotEmpty()
    }

    fun cancel() {
        logger.info("Cancelling fermentation control")
        temperatureControlFuture?.cancel(true)
        fermentationMonitorFuture?.cancel(true)
        if (getCurrentHeatingMode() != HeatingMode.OFF) {
            temperatureActuator.setHeatingMode(HeatingMode.OFF)
        }
        logger.info("Control cancelled. Heating mode set to Off")
    }
}

fun Int.numDigits() = when(this) {
    0 -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}
