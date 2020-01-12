package fermbot.profile

import fermbot.brewfather.Brewfather
import fermbot.hardwarebridge.ThermoHydrometerReader
import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.simulation.SimulationDs18b20Manager
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.BrewfatherUploadTask
import fermbot.monitor.FermentationMonitorTask
import fermbot.monitor.FermentationSnapshot
import fermbot.monitor.HeatingMode
import io.micronaut.context.env.Environment
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.TaskScheduler
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
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
                                                        private val environment: Environment,
                                                        brewfather: Optional<Brewfather>,
                                                        private val brewfatherUploadTask: BrewfatherUploadTask) {

    private var temperatureControlFuture: ScheduledFuture<*>? = null
    private var fermentationMonitorFuture: ScheduledFuture<*>? = null
    private var brewfatherUploadFuture: ScheduledFuture<*>? = null

    private val fermentationMonitorPeriod = Duration.ofMinutes(10)
    private val brewfatherAttemptPeriod = Duration.ofMinutes(2)
    private val tempControlPeriod = Duration.ofMinutes(1)

    val currentSetpoint: TemperatureSetpoint
        get() = setpointDeterminer.currentSetpoint

    private val currentProfile = profilePersister.loadOrElse(mutableListOf()).toMutableList()

    private val logger = LoggerFactory.getLogger(FermentationProfileController::class.java)

    private lateinit var setpointDeterminer: SetpointDeterminer

    val currentSetpointIndex: Int
        get() = setpointDeterminer.currentSetpointIndex

    init {
        logger.info("CurrentProfile: {}", if (currentProfile.isEmpty()) { "[empty]" } else { "\n${prettyFormatCurrentProfile()}"})
        if (Environments.SIMULATION in environment.activeNames) {
            logger.info("In simulation mode. Simulation step duration: ${determineSimulationStepDuration()}")
        }
        brewfather.ifPresent { brewfatherClient ->
            logger.info("Posting test data to brewfatherClient so device is visible")
            val result = brewfatherClient.updateBatchDetails(thermometerReader.getDevices().map { it.currentTemp }, hydrometerReader.readTilt().map { it.specificGravity }, "Fermbot Startup")
            if (result.isSuccessful()) {
                logger.info("Upload successful")
            } else {
                logger.info("Upload not successful: ${result.result}")
            }
        }
        if (thermometerReader is SimulationDs18b20Manager) { //if we're simulating temperature, pass this instance to the thermometer.
            thermometerReader.fermentationProfileController = this
        }
    }

    @Get("/profile")
    fun getProfile() : List<TemperatureSetpoint> {
        return currentProfile
    }

    @Get("/snapshots", produces=[MediaType.TEXT_PLAIN])
    fun getSnapshots(): String {
        return fermentationMonitorTask.snapshotsAsCsv.joinToString("\n")
    }

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
        currentProfile.forEachIndexed { i, currentSetpoint ->
            profileAsString.append("\t\t\t\t${padToWidth(i + 1, currentProfile.size.numDigits())}: $currentSetpoint\n") //display as one-based
        }
        return profileAsString.toString()
    }


    @Get("/status")
    fun status() : Any {
        return if (::setpointDeterminer.isInitialized) {
            setpointDeterminer.getRemainingSetpointInfo()
        } else {
            "No status to report. Fermentation not running"
        }
    }

    @Get("latest-snapshot")
    fun getLatestSnapshot() = fermentationMonitorTask.mostRecentSnapshot

    @Post("/snapshot")
    fun captureSnapshot(): FermentationSnapshot {
        val canRun = fermentationMonitorFuture != null && !fermentationMonitorFuture!!.isCancelled
        check(canRun)
        logger.info("Received request to force snapshot. Capturing now.")
        fermentationMonitorTask.run()
        return getLatestSnapshot()
    }

    /**
     * Actually starts the fermentation controller and set point
     */
    fun start() {
        setpointDeterminer = SetpointDeterminer(currentProfile, setpointCompletionPersister, fermentationMonitorTask)
        logger.info("Starting fermentation profile. Fermentation monitor period: $fermentationMonitorPeriod. Temperature control will run every $tempControlPeriod")
        val temperatureControlTask = TemperatureControlTask(
                setpointDeterminer, hydrometerReader, hysteresisProfile, thermometerReader, temperatureActuator, fermentationMonitorTask
        )
        temperatureControlTask.run()
        fermentationMonitorTask.fermentationProfileController = this
        fermentationMonitorTask.clearSnapshots()
        brewfatherUploadTask.fermentationProfileController = this
        if (Environments.SIMULATION in environment.activeNames) {
            val duration = determineSimulationStepDuration()
            temperatureControlFuture = taskScheduler.scheduleAtFixedRate(duration, duration, temperatureControlTask)
            fermentationMonitorFuture = taskScheduler.scheduleAtFixedRate(duration, duration, fermentationMonitorTask)
        } else {
            temperatureControlFuture = taskScheduler.scheduleAtFixedRate(tempControlPeriod, tempControlPeriod, temperatureControlTask)
            fermentationMonitorFuture = taskScheduler.scheduleAtFixedRate(fermentationMonitorPeriod, fermentationMonitorPeriod, fermentationMonitorTask)
            brewfatherUploadFuture = taskScheduler.scheduleAtFixedRate(brewfatherAttemptPeriod, brewfatherAttemptPeriod, brewfatherUploadTask)
        }
    }

    private fun determineSimulationStepDuration(): Duration {
        //in simulation mode, a second is equivalent to a day. Equivalent to every 10 minutes
        return environment.getProperty(FermbotProperties.simulationStep, Duration::class.java).orElse(Duration.ofMillis(10))
    }

    fun clearProfile() {
        logger.info("Clearing fermentation profile")
        currentProfile.clear()
    }

    fun getCurrentHeatingMode() = temperatureActuator.currentHeatingMode //FIXME I don't really like reaching through the rest controller

    fun isProfileSet(): Boolean {
        return currentProfile.isNotEmpty()
    }

    @Post("/nextSetpoint")
    fun nextSetpoint(): String {
        logger.info("Received request to advance to next setpoint")
        val previousSetpoint = currentSetpoint
        setpointDeterminer.advanceToNextSetpoint()
        return "Changing setpoint from $previousSetpoint to $currentSetpoint"
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
