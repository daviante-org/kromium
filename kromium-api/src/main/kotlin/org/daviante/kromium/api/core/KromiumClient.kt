package org.daviante.kromium.api.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.api.download.KromiumDownloadListener
import org.daviante.kromium.api.download.KromiumBeforeDownloadListener
import org.daviante.kromium.api.permission.KromiumPermissionListener
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.api.network.KromiumRequestInterceptor
import org.daviante.kromium.api.network.KromiumNavigationInterceptor
import org.daviante.kromium.api.ui.KromiumContextMenuListener
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumPopupListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.api.navigation.KromiumLoadErrorListener
import java.io.File

/**
 * Represents a client session in the engine.
 * A client groups multiple browsers under a shared context (e.g., shared cookies or cache).
 */
interface KromiumClient {

    /**
     * Creates a new browser instance managed by this client.
     * @param url The initial URL to load.
     * @param config Session-specific configuration for this browser instance.
     * @return A KromiumBrowser instance.
     */
    fun createBrowser(url: String, config: KromiumClientConfig = KromiumClientConfig.default()): KromiumBrowser

    /**
     * Creates a new browser instance managed by this client with an explicit UI framework.
     * @param url The initial URL to load.
     * @param framework The UI framework for off-screen rendering canvas.
     * @return A KromiumBrowser instance.
     */
    fun createBrowser(url: String, framework: KromiumUiFramework): KromiumBrowser =
        createBrowser(url, KromiumClientConfig.builder().framework(framework).build())

    /**
     * The number of active, in-flight network requests across all browsers in this client.
     */
    val inFlightRequestCount: Int

    /**
     * Whether there are any pending network requests active on this client.
     */
    fun hasPendingRequests(): Boolean

    /**
     * The number of milliseconds elapsed since the last network request completed.
     */
    fun timeSinceLastRequestMs(): Long

    /**
     * Listener for tracking global download progress.
     */
    var downloadListener: KromiumDownloadListener?

    /**
     * The default directory where downloads should be saved.
     */
    var downloadDirectory: File

    /**
     * Optional interceptor to dynamically resolve the target path before a download begins.
     * Return null to use the default path, or return an empty string "" to cancel the download.
     */
    var onBeforeDownloadListener: KromiumBeforeDownloadListener?

    /**
     * Whether to assert Do Not Track (DNT) and Global Privacy Control (Sec-GPC) headers on outbound requests.
     */
    var doNotTrack: Boolean

    /**
     * Whether permission decisions (e.g. camera, microphone) should be cached for the lifetime of this client session.
     */
    var rememberPermissions: Boolean

    /**
     * Request interceptor for filtering or modifying outgoing HTTP/HTTPS requests.
     */
    var requestInterceptor: KromiumRequestInterceptor?

    /**
     * Interceptor for evaluating and potentially overriding top-level navigation.
     */
    var navigationInterceptor: KromiumNavigationInterceptor?

    /**
     * Listener for HTTP authentication challenges.
     */
    var authListener: KromiumAuthListener?

    /**
     * Listener for popup window requests.
     */
    var popupListener: KromiumPopupListener?

    /**
     * Listener for JavaScript dialogs (alert, confirm, prompt).
     */
    var jsDialogListener: KromiumJsDialogListener?

    /**
     * Globally enables or disables native right-click context menus for this client.
     * Defaults to true. Set to false for kiosk mode or custom UI implementations.
     */
    var enableContextMenus: Boolean

    /**
     * Listener for context menu events and custom menu construction.
     */
    var contextMenuListener: KromiumContextMenuListener?

    /**
     * Configures the right-click context menu using a declarative builder block.
     */
    fun setContextMenu(block: KromiumMenuBuilder.(KromiumContextMenuContext) -> Unit)

    /**
     * Listener for media/device permission requests.
     */
    var permissionListener: KromiumPermissionListener?

    /**
     * Listener for browser console messages.
     */
    var consoleMessageListener: KromiumConsoleMessageListener?

    /**
     * Listener for page load errors.
     */
    var loadErrorListener: KromiumLoadErrorListener?

    /**
     * The currently active proxy configuration for this client session, or null if no custom proxy has been set.
     */
    val currentProxy: KromiumProxy?

    /**
     * Dynamically updates the proxy settings for this client session.
     *
     * @param proxy The new proxy configuration to apply.
     * @return true if the proxy was successfully updated, false otherwise.
     */
    fun updateProxy(proxy: KromiumProxy): Boolean

    /**
     * Clears all remembered permission decisions from the in-memory session cache.
     */
    fun clearPermissionCache()

    /**
     * Cancels an active download by its ID.
     */
    fun cancelDownload(downloadId: Int): Boolean

    /**
     * Pauses an active download by its ID.
     */
    fun pauseDownload(downloadId: Int): Boolean

    /**
     * Resumes a paused download by its ID.
     */
    fun resumeDownload(downloadId: Int): Boolean

    /**
     * Checks if a download is currently paused.
     */
    fun isDownloadPaused(downloadId: Int): Boolean

    /**
     * Disposes the client and all associated browsers.
     */
    fun dispose()
}
