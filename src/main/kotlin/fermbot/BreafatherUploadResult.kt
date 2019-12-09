package fermbot

import java.beans.ConstructorProperties

/**
 *
 * @author Zachary Richards
 * @version $ 12/5/19
 */
data class BrewfatherUploadResult @ConstructorProperties("result") constructor(val result: String) {
    fun isSuccessful(): Boolean {
        return result == "success"
    }
}