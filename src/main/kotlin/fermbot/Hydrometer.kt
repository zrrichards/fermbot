package fermbot

import java.math.BigDecimal

/**
 * @author Zachary Richards
 */
interface Hydrometer {

    /**
     * Returns specific gravity in the format of 1.095
     */
    val specificGravity: Double
}

