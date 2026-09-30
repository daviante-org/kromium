package org.daviante.kromium.api.cookie

import java.util.Date

/**
 * Represents a single HTTP cookie in the Kromium engine.
 */
data class KromiumCookie(
    val name: String,
    val value: String,
    val domain: String?,
    val path: String,
    val isSecure: Boolean,
    val isHttpOnly: Boolean,
    val hasExpires: Boolean,
    val expires: Date?
)
