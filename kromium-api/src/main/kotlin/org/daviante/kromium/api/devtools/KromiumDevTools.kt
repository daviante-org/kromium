package org.daviante.kromium.api.devtools

import java.awt.Point

/**
 * Facade for developer tools operations.
 */
interface KromiumDevTools {
    /** Opens the native Developer Tools for this browser. */
    fun openDevTools(inspectPoint: Point? = null)

    /** Closes the Developer Tools if they are open. */
    fun closeDevTools()
}
