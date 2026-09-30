package org.daviante.kromium.jcef.proxy

import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.api.proxy.KromiumProxyManager
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.jcef.core.JcefKromiumClient
import org.cef.browser.CefBrowser

internal class JcefProxyManager(
    private val cefBrowser: CefBrowser,
    private val client: JcefKromiumClient? = null
) : KromiumProxyManager {

    companion object {
        private const val TAG = "JcefProxyManager"
    }

    override fun updateProxy(proxy: KromiumProxy): Boolean {
        try {
            proxy.validate()
            val prefMap = proxy.toPreferenceMap()
            val requestContext = cefBrowser.requestContext
            if (requestContext == null) {
                KromiumLogger.e(TAG, "Failed to update proxy: CefRequestContext is null")
                return false
            }

            val error = requestContext.setPreference("proxy", prefMap)
            if (error.isNullOrEmpty()) {
                client?.currentProxy = proxy
                KromiumLogger.i(TAG, "Client proxy updated to: $proxy")
                return true
            } else {
                KromiumLogger.e(TAG, "Failed to set client proxy: $error")
                return false
            }
        } catch (t: Throwable) {
            KromiumLogger.e(TAG, "Error applying dynamic client proxy", t)
            return false
        }
    }
}
