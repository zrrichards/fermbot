package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

interface Persister<T: Any> {
    fun hasPersistedData(): Boolean
    fun read(): T
    fun persist(currentProfile: T)
}

@Singleton
class FileBasedProfilePersister @Inject constructor(private val objectMapper: ObjectMapper) : Persister<List<TemperatureSetpoint>> {

    private val logger = LoggerFactory.getLogger(FileBasedProfilePersister::class.java)
    private val currentProfileFile = Paths.get(".current-profile.json").toFile()

    override fun hasPersistedData() = currentProfileFile.exists() && currentProfileFile.readBytes().isNotEmpty()

    override fun read(): List<TemperatureSetpoint> {
        logger.info("Reading fermentation profile from: {}", currentProfileFile.absolutePath)
        return try {
            objectMapper.readValue<List<TemperatureSetpoint>>(currentProfileFile).toMutableList()
        } catch (e: Exception) {
            logger.warn("Error loading existing profile. Setting current profile to empty", e)
            mutableListOf()
        }
    }

    override fun persist(currentProfile: List<TemperatureSetpoint>) {
        logger.info("Persisting fermentation profile to: {}", currentProfileFile.absolutePath)
        if (currentProfile.isEmpty()) {
            currentProfileFile.delete()
        } else {
            currentProfileFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentProfile))
        }
    }
}
