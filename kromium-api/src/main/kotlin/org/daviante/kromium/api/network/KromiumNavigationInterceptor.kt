package org.daviante.kromium.api.network

/**
 * Functional interface to intercept and override browser navigation.
 */
fun interface KromiumNavigationInterceptor {
    /**
     * Called before browser navigation occurs.
     * @param url The target URL.
     * @param isRedirect True if this navigation is the result of a server or client redirect.
     * @return True to cancel the navigation, false to allow it.
     */
    fun shouldOverrideUrlLoading(url: String, isRedirect: Boolean): Boolean
}
