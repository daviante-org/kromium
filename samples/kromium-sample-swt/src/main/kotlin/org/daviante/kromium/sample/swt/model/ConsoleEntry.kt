package org.daviante.kromium.sample.swt.model

data class ConsoleEntry(
    val message: String,
    val source: String,
    val line: Int,
    val timestamp: String,
    val level: String = "INFO"
)
