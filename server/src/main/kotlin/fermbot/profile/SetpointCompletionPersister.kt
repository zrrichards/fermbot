package fermbot.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named(BeanDefinitions.SETPOINT_COMPLETION_PERSISTER)
class SetpointCompletionPersister @Inject constructor(private val objectMapper: ObjectMapper) : Persister<SetpointCompletion> {

    private val logger = LoggerFactory.getLogger(FileBasedProfilePersister::class.java)
    private val currentProfileFile = Paths.get(".current-setpoint-completion.json").toFile()
    override fun hasPersistedData() = currentProfileFile.exists() && currentProfileFile.readBytes().isNotEmpty()

    override fun read(): SetpointCompletion {
        logger.info("Reading setpoint completion from: {}", currentProfileFile.absolutePath)
        return objectMapper.readValue<SetpointCompletion>(currentProfileFile)
    }

    override fun persist(currentProfile: SetpointCompletion) {
        logger.info("Persisting setpoint completion to: {}", currentProfileFile.absolutePath)
        currentProfileFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentProfile))
    }

    override fun clear() {
        currentProfileFile.delete()
    }

}