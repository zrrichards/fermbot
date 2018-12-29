/**
 *
 * @author Zachary Richards
 * @version 12/10/19
 */
import react.dom.*
import kotlin.browser.document

fun main() {
    render(document.getElementById("root")) {
        h1 {
            +"Hello, React+Kotlin/JS!"
        }
    }
}