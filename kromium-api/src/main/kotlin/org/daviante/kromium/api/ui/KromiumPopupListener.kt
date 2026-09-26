package org.daviante.kromium.api.ui

/**
 * Listener for intercepting popup window creation events.
 */
fun interface KromiumPopupListener {
    /**
     * Called before a popup window is opened.
     *
     * @param targetUrl The destination URL of the popup window.
     * @return `true` to cancel/block the popup, or `false` to allow the popup to open.
     */
    fun onBeforePopup(targetUrl: String): Boolean
}
