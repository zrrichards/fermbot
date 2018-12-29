package fermbot.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * A mechanism for persisting and appending to rolling fermentation shapshot data
 * @author Zachary Richards
 * @version 12/17/19
 */
interface FermentationSnapshotPersister {
    fun append(snapshot: FermentationSnapshot)
    fun readAll(): List<FermentationSnapshot>
    fun clear()
}

@Singleton
class FileBasedFermentationSnapshotPersister @Inject constructor(private val objectMapper: ObjectMapper): FermentationSnapshotPersister {
    override fun clear() {
        file.delete()
        file.writeText(fieldsList.joinToString(",") + "\n")
    }

    private val file = Paths.get(".fermentation-snapshots").toFile()

    val fieldsList = FermentationSnapshot::class.memberProperties.map { it.name }

    override fun append(snapshot: FermentationSnapshot) {
        val snapshotMap = snapshot.asMap()
        file.appendText(fieldsList.map { snapshotMap[it]}.joinToString(",") + "\n")
    }

    override fun readAll(): List<FermentationSnapshot> {
        val constructor = FermentationSnapshot::class.primaryConstructor
        /* List<String>
         * List<List<String>>
         * List<Map<String, String>
         * List<Map<Parameter, String>>
         * List<FermentationSnapshot>
         */
        return file.readLines().map {
            fieldsList.zip(it.split(",")).toMap().mapKeys { constructor!!.parameters.find {
                param -> it.key == param.name
            } }
        }.map { constructor!!.callBy(it as Map<KParameter, Any?>) }
    }

    private inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    //todo function to determine if SG is unchanged for a certain amount of time
    //todo ensure using a cache or circular buffer in memory so list of snapshots doesn't grow too much (that would be in controller).
}