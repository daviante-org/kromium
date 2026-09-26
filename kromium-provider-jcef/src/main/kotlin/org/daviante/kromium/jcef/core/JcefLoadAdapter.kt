package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.core.automation.KromiumEmulation
import org.daviante.kromium.core.logging.KromiumLogger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import org.daviante.kromium.api.navigation.KromiumLoadError

internal class JcefLoadAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefLoadHandlerAdapter() {

    companion object {
        private const val TAG = "JcefLoadAdapter"
    }

    override fun onLoadingStateChange(
        browser: CefBrowser?,
        isLoading: Boolean,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val target = client.browsers.find { it.cefBrowser === browser || (browser != null && it.cefBrowser.identifier == browser.identifier) }
            ?: client.browsers.firstOrNull()
        target?.updateNavigationState(isLoading, canGoBack, canGoForward, browser?.url)

        client.loadHandlers.forEach {
            it.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward)
        }
    }

    override fun onLoadStart(
        browser: CefBrowser?,
        frame: CefFrame?,
        transitionType: CefRequest.TransitionType?
    ) {
        val target = client.browsers.find { it.cefBrowser === browser || (browser != null && it.cefBrowser.identifier == browser.identifier) }
            ?: client.browsers.firstOrNull()
        val currentUrl = frame?.url ?: browser?.url
        target?.updateLoading(true, currentUrl)

        if (config.emulateDesktopEnvironment) {
            injectEmulation(frame)
        }

        client.loadHandlers.forEach {
            it.onLoadStart(browser, frame, transitionType)
        }
    }

    override fun onLoadEnd(
        browser: CefBrowser?,
        frame: CefFrame?,
        httpStatusCode: Int
    ) {
        val target = client.browsers.find { it.cefBrowser === browser || (browser != null && it.cefBrowser.identifier == browser.identifier) }
            ?: client.browsers.firstOrNull()
        val currentUrl = frame?.url ?: browser?.url
        target?.updateLoading(false, currentUrl)

        if (config.emulateDesktopEnvironment) {
            injectEmulation(frame)
        }

        client.loadHandlers.forEach {
            it.onLoadEnd(browser, frame, httpStatusCode)
        }
    }

    private fun injectEmulation(frame: CefFrame?) {
        if (frame == null) return
        try {
            frame.executeJavaScript(KromiumEmulation.SCRIPT, frame.url ?: "about:blank", 0)
        } catch (e: Throwable) {
            KromiumLogger.e(TAG, "Exception injecting desktop emulation in frame", e)
        }
    }

    override fun onLoadError(
        browser: CefBrowser?,
        frame: CefFrame?,
        errorCode: CefLoadHandler.ErrorCode?,
        errorText: String?,
        failedUrl: String?
    ) {
        client.loadErrorListener?.onLoadError(
            KromiumLoadError(
                errorCode = errorCode?.code ?: 0,
                errorText = errorText ?: "Unknown error",
                failedUrl = failedUrl ?: ""
            )
        )

        client.loadHandlers.forEach {
            it.onLoadError(browser, frame, errorCode, errorText, failedUrl)
        }
    }
}
