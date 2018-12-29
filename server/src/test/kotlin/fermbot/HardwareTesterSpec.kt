package fermbot

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.hardwarebridge.DS18B20
import fermbot.hardwarebridge.ThermometerReader
import fermbot.hardwarebridge.simulation.SimulationDs18b20Manager
import fermbot.hardwarebridge.tempcontrol.HardwareBackedTemperatureActuator
import fermbot.hardwarebridge.tempcontrol.TemperatureActuator
import fermbot.monitor.HeatingMode
import fermbot.orchestrator.HardwareTester
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
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

/**
 *
 * @author Zachary Richards
 * @version 12/22/19
 */
@MicronautTest
open class HardwareTesterSpec {
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

    open class MockTemperatureActuator : TemperatureActuator {
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

    @MockBean(HardwareBackedTemperatureActuator::class)
    open fun createSpyActuator() : TemperatureActuator {
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
        val test = TemperatureControllerTestPayload(HeatingMode.HEATING, Duration.ofMillis(1))
        val currentMode = temperatureActuator.getCurrentHeatingMode()
        expectThat(currentMode).isEqualTo(HeatingMode.OFF)
        controller.testTemperatureControl(test)
        verify(exactly = 1) {
            temperatureActuator.setHeatingMode(HeatingMode.HEATING)
        }
        expectThat(temperatureActuator.getCurrentHeatingMode()).isEqualTo(HeatingMode.OFF)
    }

    @Test
    fun `can test cooling for 5 milliseconds`() {
        val test = TemperatureControllerTestPayload(HeatingMode.COOLING, Duration.ofMillis(1))
        val currentMode = temperatureActuator.getCurrentHeatingMode()
        expectThat(currentMode).isEqualTo(HeatingMode.OFF)
        controller.testTemperatureControl(test)
        verify(exactly = 1) {
            temperatureActuator.setHeatingMode(HeatingMode.COOLING)
        }
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