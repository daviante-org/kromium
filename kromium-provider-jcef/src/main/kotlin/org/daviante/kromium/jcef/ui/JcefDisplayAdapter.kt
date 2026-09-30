package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.devtools.KromiumConsoleMessage
import org.daviante.kromium.api.devtools.KromiumConsoleMessageLevel
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.jcef.core.JcefKromiumClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter

internal class JcefDisplayAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefDisplayHandlerAdapter() {

    override fun onAddressChange(browser: CefBrowser?, frame: CefFrame?, url: String?) {
        if (url.isNullOrBlank() || (frame != null && !frame.isMain)) return
        val target = client.browsers.find { it.cefBrowser === browser || (browser != null && it.cefBrowser.identifier == browser.identifier) }
            ?: client.browsers.firstOrNull()
        target?.updateUrl(url)
    }

    override fun onTitleChange(browser: CefBrowser?, title: String?) {
        val target = client.browsers.find { it.cefBrowser === browser || (browser != null && it.cefBrowser.identifier == browser.identifier) }
            ?: client.browsers.firstOrNull()
        target?.updateTitle(title ?: "")
    }

    override fun onConsoleMessage(
        browser: CefBrowser?,
        level: CefSettings.LogSeverity?,
        message: String?,
        source: String?,
        line: Int
    ): Boolean {
        val msg = message ?: return false
        val src = source ?: ""
        val listener: KromiumConsoleMessageListener = client.consoleMessageListener ?: config.consoleMessageListener ?: return false

        val consoleLevel = when (level) {
            CefSettings.LogSeverity.LOGSEVERITY_ERROR,
            CefSettings.LogSeverity.LOGSEVERITY_FATAL -> KromiumConsoleMessageLevel.ERROR
            CefSettings.LogSeverity.LOGSEVERITY_WARNING -> KromiumConsoleMessageLevel.WARNING
            CefSettings.LogSeverity.LOGSEVERITY_INFO -> KromiumConsoleMessageLevel.INFO
            CefSettings.LogSeverity.LOGSEVERITY_VERBOSE -> KromiumConsoleMessageLevel.DEBUG
            else -> KromiumConsoleMessageLevel.DEFAULT
        }

        val consoleMessage = KromiumConsoleMessage(
            level = consoleLevel,
            message = msg,
            source = src,
            line = line
        )

        return listener.onConsoleMessage(consoleMessage)
    }
}
