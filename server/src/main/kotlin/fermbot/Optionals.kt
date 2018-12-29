package fermbot

import java.util.*

/**
 * Picks the "best" choice across multiple optionals. If the first optional is present and the second optional is present,
 * the second optional is returned. If the first is present and the second is empty, return the first, if both are empty, return the second optional
 * @author Zachary Richards
 * @version 12/30/19
 */
fun <T> cascadeOptionals(optional1: Optional<out T>, optional2: Optional<out T>): Optional<T> {
    return when {
        optional2.isPresent -> optional2
        optional1.isPresent -> optional1
        else -> Optional.empty()
    } as Optional<T>
}
