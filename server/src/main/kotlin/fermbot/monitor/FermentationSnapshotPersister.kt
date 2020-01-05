package fermbot.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import fermbot.Temperature
import fermbot.temperatureFromString
import java.nio.file.Paths
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * A mechanism for persisting and appending to rolling fermentation shapshot data
 * @author Zachary Richards
 * @version 12/17/19
 */
interface FermentationSnapshotPersister {
    fun append(snapshot: FermentationSnapshot)
    fun readAll(): List<FermentationSnapshot>
    fun clear()
    fun readAsCsv(): List<String>
}

@Singleton
class FileBasedFermentationSnapshotPersister @Inject constructor(private val objectMapper: ObjectMapper): FermentationSnapshotPersister {
    override fun clear() {
        file.delete()
        file.writeText( CSV_HEADER + "\n")
    }

    private val file = Paths.get(".fermentation-snapshots").toFile()

    val fieldsMap = FermentationSnapshot::class.memberProperties.map { it.name to it.returnType.classifier as KClass<*> }.toMap()

    val fieldsList = fieldsMap.keys

    private val function: (String) -> Any = { it } //not sure why the type inference fails if this variable is inlined

    private val CSV_HEADER = fieldsList.joinToString(",")
    /**
     * The set of functions that parse the string value from the CSV to the parameters of the fermentation snapshot object
     */
    private val transformers = mapOf<KClass<*>, (String) -> Any>(
        Instant::class to Instant::parse,
        Temperature::class to ::temperatureFromString,
        Double::class to String::toDouble,
        Int::class to String::toInt,
        HeatingMode::class to HeatingMode::valueOf,
        String::class to function)

    override fun append(snapshot: FermentationSnapshot) {
        val snapshotMap = snapshot.asMap()
        file.appendText(fieldsList.map { snapshotMap[it]}.joinToString(",") + "\n")
    }

    override fun readAll(): List<FermentationSnapshot> {
        /* List<String>
         * List<List<String>>
         * List<Map<String, String>
         * List<Map<Parameter, String>>
         * List<FermentationSnapshot>
         */
        return readAsCsv().skipHeader().map {
            fieldsList.zip(it.split(",")).toMap()
        }.map { transformParameters(it) }.map { objectMapper.convertValue(it, FermentationSnapshot::class.java) }
    }

    override fun readAsCsv(): List<String> {
        val lines = file.readLines()
        check(lines[0].trim() == CSV_HEADER)
        return lines
    }


    private fun transformParameters(parameters: Map<String, String>) : Map<String, Any> {
        return parameters.mapValues {
            val kClass = fieldsMap[it.key] ?: throw IllegalArgumentException("Unrecognized parameter named ${it.key}")
            val transformFunction = transformers[kClass] ?: throw IllegalArgumentException("No transform function found for type: $kClass")
            transformFunction(it.value)
        }
    }

    private inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    //todo function to determine if SG is unchanged for a certain amount of time
    //todo ensure using a cache or circular buffer in memory so list of snapshots doesn't grow too much (that would be in controller).
}

private fun <E> List<E>.skipHeader(): List<E> = subList(1, size)
