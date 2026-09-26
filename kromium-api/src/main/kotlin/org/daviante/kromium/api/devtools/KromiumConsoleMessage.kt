package org.daviante.kromium.api.devtools

/**
 * A message logged to the browser's JavaScript developer console.
 *
 * @property level The severity level of the console message.
 * @property message The text content of the message.
 * @property source The URL or script origin that emitted the message.
 * @property line The line number in the source file where the message was emitted (1-based), or 0 if unknown.
 */
data class KromiumConsoleMessage(
    val level: KromiumConsoleMessageLevel,
    val message: String,
    val source: String = "",
    val line: Int = 0
)
