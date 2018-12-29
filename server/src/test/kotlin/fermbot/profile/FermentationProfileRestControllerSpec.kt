package fermbot.profile

import fermbot.toF
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
@MicronautTest
class FermentationProfileRestControllerSpec {

    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    lateinit var controller: FermentationProfileRestController

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @BeforeEach
    fun clearProfile() {
        controller.clearProfile()
    }

    @Replaces(RaspberryPiProfilePersister::class)
    @Singleton
    class MockPersister : ProfilePersister { //prevents creating the json file on the filesystem
        var persistedProfile = mutableListOf<TemperatureSetpoint>()

        override fun hasPersistedProfile(): Boolean {
            return persistedProfile.isNotEmpty()
        }

        override fun readProfile(): List<TemperatureSetpoint> {
            return persistedProfile
        }

        override fun persistProfile(currentProfile: MutableList<TemperatureSetpoint>) {
            persistedProfile = currentProfile
        }

    }

    @Test
    fun `by default the fermentation profile is empty`() {
        val req = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list.size).isEqualTo(0)
    }

    @Test
    fun `can add a single fermentation profile stage`() {
        val profileList = listOf(SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "foo stage"))
        val req = POST("/profile", profileList)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        val req2 = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req2, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list).isEqualTo(profileList)
    }

    @Test
    fun `can add a mix of fermentation profile stages`() {
        val profileList = listOf(
                SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "foo stage"),
                TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "foo stage", true)
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
                "tempSetpoint": "48F",
                "untilSg": 1.023,
                "stageDescription": "Primary"
            },
            {
                "tempSetpoint": "62F",
                "duration": "P2D",
                "stageDescription": "Diacetyl Rest",
                "includeRamp": false
            },
            {
                "tempSetpoint": "34F",
                "duration": "P14D",
                "stageDescription": "Cold Crash",
                "includeRamp": true
            }
        ]""".trimIndent()


        val profileList = listOf(
                SpecificGravityBasedSetpoint(48.0.toF(), 1.023, "Primary"),
                TimeBasedSetpoint(62.0.toF(), Duration.ofDays(2), "Diacetyl Rest", false),
                TimeBasedSetpoint(34.0.toF(), Duration.ofDays(14), "Cold Crash", true)
        )

        val req = POST("/profile", json)
        val resp = client.toBlocking().exchange(req, String::class.java)
        expectThat(resp.status).isEqualTo(HttpStatus.OK)

        val req2 = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req2, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list).isEqualTo(profileList)

    }
}

