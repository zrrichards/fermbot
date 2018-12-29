package fermbot

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.DS18B20Manager
import fermbot.hardwarebridge.raspberrypi.RaspberryPiDS18B20Manager
import fermbot.hardwarebridge.tempcontrol.HardwareBackedTemperatureActuator
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.HeatingMode
import fermbot.orchestrator.HardwareTester
import fermbot.orchestrator.HeatingTestPayload
import fermbot.profile.TemperatureControllerTestPayload
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@MicronautTest
class HardwareTesterSpec {
    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    lateinit var controller: HardwareTester

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Inject
    private lateinit var temperatureActuator: TemperatureActuator

    @Inject
    private lateinit var objectMapper: ObjectMapper

    class MockTemperatureActuator : TemperatureActuator {
        private var currentHeatingMode = HeatingMode.OFF

        override fun setHeatingMode(heatingMode: HeatingMode): HeatingMode {
            val tmp = currentHeatingMode
            currentHeatingMode = heatingMode
            return tmp
        }

        override fun getCurrentHeatingMode(): HeatingMode {
            return currentHeatingMode
        }
    }

    class MockDs18b20Manager : DS18B20Manager {
        override fun getDevices(): DS18B20 {
            return DS18B20("foo-id", (Random.nextInt(320, 1000) / 10.0).toF(), Instant.now())
        }
    }

    @MockBean(RaspberryPiDS18B20Manager::class)
    fun createManager() : DS18B20Manager {
        return MockDs18b20Manager()
    }

    @MockBean(HardwareBackedTemperatureActuator::class)
    fun createSpyActuator() : TemperatureActuator {
        return spyk(MockTemperatureActuator())
    }

    @Test
    fun `test full hardware`() {
        val req = HttpRequest.POST("/test/full-hardware","""{"stepDuration":"PT0.001S"}""")
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `can test heat for 5 milliseconds`() {
        val test = TemperatureControllerTestPayload(HeatingMode.HEATING, Duration.ofMillis(5))
        val currentMode = temperatureActuator.getCurrentHeatingMode()
        expectThat(currentMode).isEqualTo(HeatingMode.OFF)
        controller.testTemperatureControl(test)
        verify(exactly = 1) {
            temperatureActuator.setHeatingMode(HeatingMode.HEATING)
        }
        //todo mock "sleeper"
        expectThat(temperatureActuator.getCurrentHeatingMode()).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can test cooling for 5 milliseconds`() {
        val test = TemperatureControllerTestPayload(HeatingMode.COOLING, Duration.ofMillis(5))
        val currentMode = temperatureActuator.getCurrentHeatingMode()
        expectThat(currentMode).isEqualTo(HeatingMode.OFF)
        controller.testTemperatureControl(test)
        verify(exactly = 1) {
            temperatureActuator.setHeatingMode(HeatingMode.COOLING)
        }
        //todo mock "sleeper"
        expectThat(temperatureActuator.getCurrentHeatingMode()).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can use post request to test heating`() {
        val json = """{"mode":"HEATING","duration":"PT0.005S"}"""
        val req = HttpRequest.POST("/test/heating-mode", json)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        verify(exactly = 1) {
            temperatureActuator.setHeatingMode(HeatingMode.HEATING)
        }
        //todo mock "sleeper"
        expectThat(temperatureActuator.getCurrentHeatingMode()).isEqualTo(HeatingMode.OFF)
    }
}