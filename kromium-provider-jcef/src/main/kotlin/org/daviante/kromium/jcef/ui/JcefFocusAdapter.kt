package org.daviante.kromium.jcef.ui

import org.cef.browser.CefBrowser
import org.cef.handler.CefFocusHandler
import org.cef.handler.CefFocusHandlerAdapter

import org.cef.browser.CefBrowserOsr

/**
 * Exact focus adapter matching the tested implementation in Kromium OLD code.
 */
class JcefFocusAdapter : CefFocusHandlerAdapter() {

    override fun onTakeFocus(browser: CefBrowser?, next: Boolean) {
    }

    override fun onSetFocus(browser: CefBrowser?, source: CefFocusHandler.FocusSource?): Boolean {
        return false
    }

    override fun onGotFocus(browser: CefBrowser?) {
        browser?.uiComponent?.requestFocusInWindow()
        (browser as? CefBrowserOsr)?.osrComponent?.requestFocus()
    }
}
