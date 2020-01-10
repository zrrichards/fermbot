package fermbot.brewfather

import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.env.Environment
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 1/10/20
 */
@Context
class ConfigurationValidator @Inject constructor (private val environment: Environment) {
    init { validateConfiguration() }

    private fun validateConfiguration() {
        val tiltEnabled = environment.getProperty(FermbotProperties.isTiltEnabled, Boolean::class.java).orElse(false)
        val ds18b20Enabled = environment.getProperty(FermbotProperties.isDs18b20Enabled, Boolean::class.java).orElse(false)
        check (tiltEnabled || ds18b20Enabled) { "Must have at least one thermometer device enabled. Set ${FermbotProperties.isTiltEnabled} or ${FermbotProperties.isDs18b20Enabled} to true" }
    }
}