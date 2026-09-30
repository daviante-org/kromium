package org.daviante.kromium.api.core

import java.io.File

/**
 * Provides information about the installed Kromium Engine.
 */
data class KromiumEngineInfo(
    val installDir: File,
    val isInstalled: Boolean,
    val engineVersion: String,
    val browserVersion: String,
    val providerMetadata: Map<String, String> = emptyMap()
)
