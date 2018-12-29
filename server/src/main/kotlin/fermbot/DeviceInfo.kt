package fermbot

import io.micronaut.context.annotation.Property
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
@Singleton
class DeviceInfo @Inject constructor(@Property(name="micronaut.application.name") appName: String, @Property(name="fermbot.suffix") suffix: String) {
    val deviceName = appName + "_$suffix"
}