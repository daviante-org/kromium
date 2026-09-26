package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.ui.KromiumContextMenuParams
import org.daviante.kromium.jcef.core.JcefKromiumClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandlerAdapter
import java.util.concurrent.ConcurrentHashMap

internal class JcefContextMenuAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefContextMenuHandlerAdapter() {

    private val contextMenuActions = ConcurrentHashMap<Int, (KromiumContextMenuContext) -> Unit>()
    @Volatile private var activeContextMenuContext: KromiumContextMenuContext? = null

    private fun mapParams(params: CefContextMenuParams?): KromiumContextMenuParams {
        if (params == null) {
            return KromiumContextMenuParams(
                x = 0,
                y = 0
            )
        }
        return KromiumContextMenuParams(
            x = params.xCoord,
            y = params.yCoord,
            linkUrl = params.linkUrl?.takeIf { it.isNotEmpty() },
            unfilteredLinkUrl = params.unfilteredLinkUrl?.takeIf { it.isNotEmpty() },
            sourceUrl = params.sourceUrl?.takeIf { it.isNotEmpty() },
            hasImage = params.hasImageContents(),
            pageUrl = params.pageUrl?.takeIf { it.isNotEmpty() },
            frameUrl = params.frameUrl?.takeIf { it.isNotEmpty() },
            selectionText = params.selectionText?.takeIf { it.isNotEmpty() },
            misspelledWord = params.misspelledWord?.takeIf { it.isNotEmpty() },
            isEditable = params.isEditable,
            isSpellCheckEnabled = params.isSpellCheckEnabled
        )
    }

    override fun onBeforeContextMenu(
        browser: CefBrowser?,
        frame: CefFrame?,
        params: CefContextMenuParams?,
        model: CefMenuModel?
    ) {
        if (!client.enableContextMenus) {
            model?.clear()
            return
        }

        val listener = client.contextMenuListener ?: config.contextMenuListener
        if (listener != null && model != null) {
            contextMenuActions.clear()
            val kBrowser = client.browsers.find { it.cefBrowser == browser }
            val contextParams = mapParams(params)
            val ctx = KromiumContextMenuContext(kBrowser, contextParams)
            activeContextMenuContext = ctx

            val builder = JcefMenuBuilder(model, ctx, contextMenuActions)
            try {
                listener.onBuildContextMenu(builder, ctx)
            } catch (_: Throwable) {
                // Ignore context menu construction failure
            }
        }
    }

    override fun onContextMenuCommand(
        browser: CefBrowser?,
        frame: CefFrame?,
        params: CefContextMenuParams?,
        commandId: Int,
        eventFlags: Int
    ): Boolean {
        val action = contextMenuActions[commandId]
        if (action != null) {
            val kBrowser = client.browsers.find { it.cefBrowser == browser }
            val ctx = activeContextMenuContext ?: KromiumContextMenuContext(kBrowser, mapParams(params))
            try {
                action.invoke(ctx)
            } catch (_: Throwable) {
                // Non-critical action callback failure
            }
            return true
        }
        return false
    }

    override fun onContextMenuDismissed(browser: CefBrowser?, frame: CefFrame?) {
        activeContextMenuContext = null
    }
}
