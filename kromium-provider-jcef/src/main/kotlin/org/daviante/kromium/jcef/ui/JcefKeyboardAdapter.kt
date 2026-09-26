package org.daviante.kromium.jcef.ui

import org.cef.browser.CefBrowser
import org.cef.handler.CefKeyboardHandler
import org.cef.handler.CefKeyboardHandlerAdapter
import org.cef.misc.BoolRef

/**
 * Exact keyboard adapter matching the tested implementation in Kromium OLD code.
 */
class JcefKeyboardAdapter : CefKeyboardHandlerAdapter() {

    override fun onPreKeyEvent(
        browser: CefBrowser?,
        event: CefKeyboardHandler.CefKeyEvent?,
        isKeyboardShortcut: BoolRef?
    ): Boolean {
        if (browser != null && event != null && JcefShortcutHandler.handleCefKeyEvent(browser, event)) {
            isKeyboardShortcut?.set(true)
            return true
        }
        return false
    }

    override fun onKeyEvent(
        browser: CefBrowser?,
        event: CefKeyboardHandler.CefKeyEvent?
    ): Boolean {
        return false
    }
}
