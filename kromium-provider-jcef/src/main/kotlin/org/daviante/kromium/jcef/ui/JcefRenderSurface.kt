package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.ui.KromiumRenderSurface
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * JCEF implementation of the rendering surface.
 * Adapts the internal CefBrowser's UI component to the generic API.
 */
class JcefRenderSurface internal constructor(
    private val cefBrowser: CefBrowser
) : KromiumRenderSurface {

    override fun <T : Any> unwrap(clazz: KClass<T>): T? {
        if (clazz.isInstance(cefBrowser)) {
            return clazz.cast(cefBrowser)
        }
        val osr = cefBrowser as? CefBrowserOsr
        val ui = osr?.uiComponentObject ?: cefBrowser.uiComponent
        if (ui != null && clazz.isInstance(ui)) {
            return clazz.cast(ui)
        }
        val comp = osr?.osrComponent
        if (comp != null && clazz.isInstance(comp)) {
            return clazz.cast(comp)
        }
        return null
    }
}
