package org.daviante.kromium.api.ui

import org.daviante.kromium.api.core.KromiumBrowser
import java.awt.Desktop
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Context passed to context menu action callbacks when an item is triggered by the user.
 *
 * Exposes reference to the active [KromiumBrowser] instance, clicked parameters, and turnkey helpers
 * (clipboard, DevTools inspect, web search, downloads).
 *
 * @property browser High-level [KromiumBrowser] instance, if resolved.
 * @property params Contextual parameters of the right-click event.
 */
class KromiumContextMenuContext(
    val browser: KromiumBrowser?,
    val params: KromiumContextMenuParams
) {
    /**
     * Copies the given text to the system clipboard.
     */
    fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (_: Throwable) {
            // Non-critical clipboard failure
        }
    }

    /**
     * Opens Chromium DevTools targeting the clicked element coordinates.
     */
    fun inspectElement() {
        try {
            browser?.devTools?.openDevTools(Point(params.x, params.y))
        } catch (_: Throwable) {
            // Ignored if DevTools cannot be opened
        }
    }

    /**
     * Initiates a download for the given URL using Chromium's download pipeline.
     */
    fun startDownload(url: String) {
        try {
            browser?.downloads?.startDownload(url)
        } catch (_: Throwable) {
            // Ignored if download fails to start
        }
    }

    /**
     * Performs a web search using the provided query and template.
     *
     * @param query The search query (defaults to highlighted [KromiumContextMenuParams.selectionText]).
     * @param engineUrl Search URL template formatted with `%s` for query insertion.
     * @param openInSystemBrowser If true, attempts to open in the OS default desktop browser; otherwise loads in [browser].
     */
    fun searchWeb(
        query: String = params.selectionText ?: "",
        engineUrl: String = "https://www.google.com/search?q=%s",
        openInSystemBrowser: Boolean = true
    ) {
        if (query.isBlank()) return
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val targetUrl = engineUrl.replace("%s", encoded)

        if (openInSystemBrowser && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI.create(targetUrl))
                return
            } catch (_: Throwable) {
                // Fall back to embedded browser navigation
            }
        }

        browser?.navigation?.loadUrl(targetUrl)
    }
}
