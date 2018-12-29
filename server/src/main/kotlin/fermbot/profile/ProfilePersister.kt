package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

interface ProfilePersister {
    fun hasPersistedProfile(): Boolean
    fun readProfile(): List<TemperatureSetpoint>
    fun persistProfile(currentProfile: List<TemperatureSetpoint>)
}

@Singleton
class RaspberryPiProfilePersister @Inject constructor(private val objectMapper: ObjectMapper) : ProfilePersister {

    private val logger = LoggerFactory.getLogger(RaspberryPiProfilePersister::class.java)
    private val currentProfileFile = Paths.get(".current-profile.json").toFile()
    override fun hasPersistedProfile() = currentProfileFile.exists()

    override fun readProfile(): List<TemperatureSetpoint> {
        logger.info("Reading fermentation profile from: {}", currentProfileFile.absolutePath)
        return try {
            objectMapper.readValue<List<TemperatureSetpoint>>(currentProfileFile).toMutableList()
        } catch (e: Exception) {
            logger.warn("Error loading existing profile. Setting current profile to empty", e)
            mutableListOf()
        }
    }

    override fun persistProfile(currentProfile: List<TemperatureSetpoint>) {
        logger.info("Persisting fermentation profile to: {}", currentProfileFile.absolutePath)
        currentProfileFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentProfile))
    }
}
