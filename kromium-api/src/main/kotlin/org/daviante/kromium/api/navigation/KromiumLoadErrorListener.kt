package org.daviante.kromium.api.navigation

/**
 * Listener invoked when a page load error occurs.
 */
fun interface KromiumLoadErrorListener {
    fun onLoadError(error: KromiumLoadError)
}
