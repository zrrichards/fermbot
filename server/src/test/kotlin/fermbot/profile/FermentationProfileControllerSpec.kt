package fermbot.profile

import fermbot.StateController
import fermbot.hardwarebridge.*
import fermbot.hardwarebridge.tempcontrol.HardwareBackedTemperatureActuator
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.FermentationMonitorTask
import fermbot.monitor.HeatingMode
import fermbot.toF
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.env.Environment
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
//@Disabled
@MicronautTest
@Property(name="fermbot.tilt.enabled", value="true")
class FermentationProfileControllerSpec {

    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    lateinit var controller: FermentationProfileController

    @Inject lateinit var stateController: StateController

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Inject
    private lateinit var environment: Environment

    @BeforeEach
    fun clearProfile() {
        stateController.returnToPendingProfile()
    }

    @Inject private lateinit var temperatureActuator: TemperatureActuator

    @Replaces(FileBasedProfilePersister::class)
    @Named(BeanDefinitions.PROFILE_PERSISTER)
    @Singleton
    class MockPersister(private var persistedProfile: MutableList<TemperatureSetpoint> = mutableListOf()) : Persister<List<TemperatureSetpoint>> {
        override fun clear() = persistedProfile.clear()

        //prevents creating the json file on the filesystem

        override fun hasPersistedData() = persistedProfile.isNotEmpty()

        override fun read() = persistedProfile

        override fun persist(currentProfile: List<TemperatureSetpoint>) {
            persistedProfile = currentProfile.toMutableList()
        }
    }

    class MockTemperatureActuator : TemperatureActuator {
        override var currentHeatingMode = HeatingMode.OFF
            private set

        override fun setHeatingMode(heatingMode: HeatingMode): HeatingMode {
            val tmp = currentHeatingMode
            currentHeatingMode = heatingMode
            return tmp
        }
    }

    @MockBean(HardwareBackedTemperatureActuator::class)
    fun createSpyActuator() : TemperatureActuator {
        return spyk(MockTemperatureActuator())
    }

    @Test
    fun `by default the fermentation profile is empty`() {
        val req = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list.size).isEqualTo(0)
    }

    @Test
    fun `can add a single fermentation profile setpoint`() {
        val profileList = listOf(SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "foo setpoint"))
        val req = POST("/profile", profileList)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        val req2 = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req2, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list).isEqualTo(profileList)
    }

    @Test
    fun `can add a mix of fermentation profile setpoints`() {
        val profileList = listOf(
                SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "foo setpoint"),
                TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "foo setpoint", true)
        )
        val req = POST("/profile", profileList)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        val req2 = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req2, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list).isEqualTo(profileList)
    }

    @Test
    fun `can create profile with specific json string`() {
        val json =
    """[
            {
                "temperature": "48F",
                "untilSg": 1.023,
                "description": "Primary"
            },
            {
                "temperature": "62F",
                "duration": "P2D",
                "description": "Diacetyl Rest",
                "includeRamp": true
            },
            {
                "temperature": "34F",
                "duration": "P14D",
                "description": "Cold Crash",
                "includeRamp": true
            }
        ]""".trimIndent()


        val profileList = listOf(
                SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary"),
                TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "Diacetyl Rest", true),
                TimeBasedSetpoint(34.0.toF(), Duration.ofDays(14), "Cold Crash", true)
        )

        val req = POST("/profile", json)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        val req2 = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req2, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list).isEqualTo(profileList)

    }



    @Test
    fun `persisted profiles are loaded at startup`() { //TODO assert that brewfather was called on startup
        val persistedProfile: MutableList<TemperatureSetpoint> = mutableListOf(
                SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary")
        )
        val mockPersister = MockPersister(persistedProfile)
        val mockMonitor = mockk<FermentationMonitorTask>(relaxed=true)
        val thermometerReader = mockk<ThermometerReader>()
        every { thermometerReader.getDevices() } returns (Optional.of(DS18B20("test-id", 45.5.toF())))
        val hydrometerReader = mockk<ThermoHydrometerReader>()
        every { hydrometerReader.readTilt() } returns(Optional.of(Tilt(TiltColors.BLACK, 1.098, 45.9)))
        val profileController = FermentationProfileController(mockPersister, mockk(), hydrometerReader, mockk(), thermometerReader, mockk(), mockMonitor, mockk(relaxed=true), environment, Optional.of(mockk(relaxed=true)), mockk())

        expectThat(profileController.getProfile()).isEqualTo(persistedProfile)
    }

}

