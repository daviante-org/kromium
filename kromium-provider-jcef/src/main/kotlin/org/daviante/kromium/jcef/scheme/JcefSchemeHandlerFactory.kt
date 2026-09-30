package org.daviante.kromium.jcef.scheme

import org.daviante.kromium.api.scheme.KromiumAssetHandler
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest

/**
 * JCEF scheme handler factory that instantiates [JcefResourceHandlerAdapter]
 * for requests routed to a [KromiumAssetHandler].
 */
internal class JcefSchemeHandlerFactory(
    private val assetHandler: KromiumAssetHandler
) : CefSchemeHandlerFactory {

    override fun create(
        browser: CefBrowser?,
        frame: CefFrame?,
        schemeName: String?,
        request: CefRequest?
    ): CefResourceHandler {
        return JcefResourceHandlerAdapter(assetHandler)
    }
}
