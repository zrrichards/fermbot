package fermbot.brewfather

import fermbot.profile.BeanDefinitions
import fermbot.profile.FermbotProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.env.Environment
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 *
 * @author Zachary Richards
 * @version 1/10/20
 */
@Context
class ConfigurationValidator @Inject constructor (private val environment: Environment) {

    private val logger = LoggerFactory.getLogger(ConfigurationValidator::class.java)

    init { validateConfiguration() }

    private var isUsable = false

    private fun validateConfiguration() {
        val tiltEnabled = environment.getProperty(FermbotProperties.isTiltEnabled, Boolean::class.java).orElse(false)
        val ds18b20Enabled = environment.getProperty(FermbotProperties.isDs18b20Enabled, Boolean::class.java).orElse(false)
        isUsable = if (!(tiltEnabled || ds18b20Enabled)) {
            logger.warn("To monitor a fermentation you must have at least one thermometer device enabled. Set ${FermbotProperties.isTiltEnabled} or ${FermbotProperties.isDs18b20Enabled} to true. Only will be able to test hardware")
            false
        } else {
            true
        }
    }

    fun isUsable() = isUsable
}