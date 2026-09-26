package org.daviante.kromium.sample.swing.model

data class ConsoleMessage(
    val message: String,
    val source: String,
    val line: Int,
    val timestamp: String
)
