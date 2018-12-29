package fermbot

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import javax.inject.Singleton

/**
 * The fermbot is a state machine with REST calls changing states. This sealed class manages these states
 * and contains which states are valid to transition to from the current state. This ensures that the Fermbot's
 * actions make sense (i.e. not testing hardware configuration in the middle of monitoring a fermentation)
 * @author Zachary Richards
 * @version 12/30/19
 */
sealed class State {

    //needs to be sealed class instead of enum because these are self-referential

    abstract val validNextStages: List<State>
    abstract val name: String

    override fun toString() = name

    fun isValidNextState(state: State) = (state == this || state in validNextStages)

    open fun persist() { /* do nothing by default */ }

    object PENDING_PROFILE: State() {
        override fun persist() {
        }

        override val validNextStages = listOf(TESTING, READY)
        override val name = "Pending Profile"
    }

    object READY: State() {
        override fun persist() {
        }
        override val validNextStages = listOf(PENDING_PROFILE, RUNNING)
        override val name = "Ready"
    }

    object TESTING: State() {
        override val validNextStages = listOf(PENDING_PROFILE)
        override val name = "Testing"
    }

    object RUNNING: State() {
        override fun persist() {
        }
        override val validNextStages = listOf(PENDING_PROFILE, READY)
        override val name = "Running"
    }
}

/**
 * Returns a list of all valid states in the system
 */
fun validStates() = State::class.nestedClasses.map { it.objectInstance as State }

@Singleton
class StateSerializer : JsonSerializer<State>() {
    override fun serialize(value: State, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.name)
    }
}

@Singleton
class StateDeserializer : JsonDeserializer<State>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = validStates().first { it.name == p.valueAsString }
}