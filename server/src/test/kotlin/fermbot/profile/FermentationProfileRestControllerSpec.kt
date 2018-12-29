package fermbot.profile

import fermbot.fermentationprofile.TemperatureSetpoint
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import javax.inject.Inject

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
    @field:Client("/")
    private lateinit var client: HttpClient

    @Test
    fun `by default the fermentation profile is empty`() {
        val req = GET<Any>("/profile")
        val list = client.toBlocking().retrieve(req, Argument.listOf(TemperatureSetpoint::class.java))
        expectThat(list.size).isEqualTo(0)
    }
}