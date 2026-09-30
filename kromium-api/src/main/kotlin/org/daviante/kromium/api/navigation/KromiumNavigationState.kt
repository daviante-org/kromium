package org.daviante.kromium.api.navigation

/**
 * Represents the current navigation and loading state of the browser.
 */
data class KromiumNavigationState(
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val url: String = "",
    val title: String = ""
)
