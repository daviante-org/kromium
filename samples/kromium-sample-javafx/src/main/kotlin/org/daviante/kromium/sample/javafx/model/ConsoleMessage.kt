package org.daviante.kromium.sample.javafx.model

/**
 * Model representing a Chromium console message.
 */
data class ConsoleMessage(
    val message: String,
    val source: String,
    val line: Int,
    val timestamp: String
)
