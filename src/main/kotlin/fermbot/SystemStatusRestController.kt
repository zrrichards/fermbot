package fermbot

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version $ 12/8/19
 */
@Controller("/status")
class SystemStatusRestController {

    @Inject
    private lateinit var systemStatistics: SystemStatistics

    @Get("/")
    fun getStatus() : Any {
       return systemStatistics.toString()
    }
}