package org.daviante.kromium.api.navigation

/**
 * Represents a failure that occurred during page navigation or loading.
 */
data class KromiumLoadError(
    val errorCode: Int,
    val errorText: String,
    val failedUrl: String
)
