package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumPopupListener
import org.daviante.kromium.core.logging.KromiumLogger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter

internal class JcefLifeSpanAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefLifeSpanHandlerAdapter() {

    companion object {
        private const val TAG = "JcefLifeSpanAdapter"
    }

    override fun onBeforePopup(
        browser: CefBrowser?,
        frame: CefFrame?,
        target_url: String?,
        target_frame_name: String?
    ): Boolean {
        val url = target_url ?: ""
        val listener: KromiumPopupListener? = client.popupListener ?: config.popupListener
        return if (listener != null) {
            try {
                listener.onBeforePopup(url)
            } catch (e: Throwable) {
                KromiumLogger.e(TAG, "Popup listener threw an exception for $url", e)
                true
            }
        } else {
            false
        }
    }
}
