package fermbot.profile

import fermbot.cascadeOptionals
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

/**
 *
 * @author Zachary Richards
 * @version 12/30/19
 */
class OptionalsSpec {

    @Test
    fun `test cascading two empty optionals results in an empty optional`() {
        expectThat(cascadeOptionals(Optional.empty<Any>(), Optional.empty())).isEqualTo(Optional.empty())
    }

    @Test
    fun `if the first optional is present and the second is empty, the first is returned`() {
        expectThat(cascadeOptionals("a".toOptional(), Optional.empty())).isEqualTo("a".toOptional())
    }

    @Test
    fun `if the first optional is empty and the second is present, the second is returned`() {
        expectThat(cascadeOptionals(Optional.empty(), "a".toOptional())).isEqualTo("a".toOptional())
    }

    @Test
    fun `if both are present, the second is returned`() {
        expectThat(cascadeOptionals("a".toOptional(), "b".toOptional())).isEqualTo("b".toOptional())
    }
}