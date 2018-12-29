package fermbot.fermentationprofile

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

/**
 *
 * @author Zachary Richards
 * @version 12/11/19
 */
@Controller("/profile")
class FermentationProfileController {

    @Get("/")
    fun getProfile() : List<TemperatureSetpoint> {
        return listOf()
    }
}