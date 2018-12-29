package fermbot

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
@Singleton
class DeviceInfo @Inject constructor(@Value("\${micronaut.application.name:fermbot}") appName: String, @Value("\${fermbot.suffix:test}") suffix: String) {
    val deviceName = appName + "_$suffix"
}