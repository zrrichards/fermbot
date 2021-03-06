package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fermbot.State
import fermbot.fromName
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface Persister<T: Any> {

    fun hasPersistedData(): Boolean
    fun read(): T
    fun persist(currentProfile: T)
    fun loadOrElse(ifNoData: T) = if (hasPersistedData()) { read() } else { ifNoData }
    fun clear()
}

@Singleton
@Named(BeanDefinitions.PROFILE_PERSISTER)
class FileBasedProfilePersister @Inject constructor(private val objectMapper: ObjectMapper) : Persister<List<TemperatureSetpoint>> {
    override fun clear() {
        currentProfileFile.delete()
    }

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

@Singleton
@Named(BeanDefinitions.STATE_PERSISTER)
class FileBasedStatePersister @Inject constructor (private val objectMapper: ObjectMapper) : Persister<State> {

    private val currentProfileFile = Paths.get(".current-state").toFile()

    override fun hasPersistedData(): Boolean {
        return try {
            read()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun read(): State {
        return objectMapper.readValue(currentProfileFile.readText())
    }

    override fun persist(currentProfile: State) {
        currentProfileFile.writeText(objectMapper.writeValueAsString(currentProfile))
    }

    override fun clear() {
        currentProfileFile.delete()
    }

}