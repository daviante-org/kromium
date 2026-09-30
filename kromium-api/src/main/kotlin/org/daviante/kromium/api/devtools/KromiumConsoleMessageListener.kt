package org.daviante.kromium.api.devtools

/**
 * Listener for JavaScript console messages emitted by the browser.
 */
fun interface KromiumConsoleMessageListener {
    /**
     * Invoked when a console message is logged.
     *
     * @param message The structured console message details including severity level.
     * @return true to indicate the message was handled and suppress default logging, or false to allow standard output.
     */
    fun onConsoleMessage(message: KromiumConsoleMessage): Boolean
}
