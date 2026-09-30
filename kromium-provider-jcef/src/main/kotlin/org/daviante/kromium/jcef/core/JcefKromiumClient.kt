package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.network.KromiumNavigationInterceptor
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.api.download.KromiumDownloadListener
import org.daviante.kromium.api.download.KromiumBeforeDownloadListener
import org.daviante.kromium.api.navigation.KromiumLoadErrorListener
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.KromiumRequestInterceptor
import org.daviante.kromium.api.permission.KromiumPermissionListener
import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.api.error.KromiumException
import org.daviante.kromium.api.ui.KromiumContextMenuListener
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumPopupListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.core.download.KromiumDownloadManager
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.jcef.automation.JcefJsHandler
import org.daviante.kromium.jcef.download.JcefDownloadAdapter
import org.daviante.kromium.jcef.network.JcefRequestAdapter
import org.daviante.kromium.jcef.permission.JcefPermissionAdapter
import org.daviante.kromium.jcef.ui.JcefContextMenuAdapter
import org.daviante.kromium.jcef.ui.JcefDisplayAdapter
import org.daviante.kromium.jcef.ui.JcefFocusAdapter
import org.daviante.kromium.jcef.ui.JcefJSDialogAdapter
import org.daviante.kromium.jcef.ui.JcefKeyboardAdapter
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserFactory
import org.cef.browser.CefBrowserOsr
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefRendering
import org.cef.browser.CefRequestContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import org.cef.handler.CefLoadHandler

/**
 * JCEF implementation of the KromiumClient.
 */
class JcefKromiumClient internal constructor(
    private val engine: JcefKromiumEngine,
    private val cefClient: CefClient,
    private val isHeadless: Boolean,
    private val config: KromiumClientConfig
) : KromiumClient {

    companion object {
        private const val TAG = "JcefKromiumClient"
    }

    internal val jsHandler = JcefJsHandler()
    internal val downloadManager = KromiumDownloadManager()
    private val permissionCache = ConcurrentHashMap<String, Int>()

    @Volatile
    override var currentProxy: KromiumProxy? = null
        internal set

    internal val htmlPayloads = ConcurrentHashMap<String, String>()
    private val htmlPayloadKeys = ConcurrentLinkedDeque<String>()

    /**
     * Registers an in-memory synthetic HTML payload, capping cache size to prevent memory leaks.
     */
    internal fun registerHtmlPayload(url: String, html: String, maxCapacity: Int = 50) {
        htmlPayloads[url] = html
        htmlPayloadKeys.remove(url)
        htmlPayloadKeys.add(url)
        while (htmlPayloadKeys.size > maxCapacity) {
            val oldest = htmlPayloadKeys.pollFirst()
            if (oldest != null) {
                htmlPayloads.remove(oldest)
            }
        }
    }

    // Network request tracking
    private val activeRequests = ConcurrentHashMap<Long, Boolean>()
    
    @Volatile
    private var lastRequestCompleteTimeMs = System.currentTimeMillis()

    override val inFlightRequestCount: Int
        get() = activeRequests.size

    override fun hasPendingRequests(): Boolean = activeRequests.isNotEmpty()

    override fun timeSinceLastRequestMs(): Long = System.currentTimeMillis() - lastRequestCompleteTimeMs

    internal fun trackRequestStart(id: Long) {
        activeRequests[id] = true
    }

    internal fun trackRequestEnd(id: Long) {
        activeRequests.remove(id)
        lastRequestCompleteTimeMs = System.currentTimeMillis()
    }

    @Volatile override var downloadListener: KromiumDownloadListener? = config.downloadListener
    @Volatile override var downloadDirectory: File = File(System.getProperty("user.home"), "Downloads")
    @Volatile override var onBeforeDownloadListener: KromiumBeforeDownloadListener? = config.onBeforeDownloadListener

    @Volatile override var doNotTrack: Boolean = config.doNotTrack
    @Volatile override var rememberPermissions: Boolean = true
    @Volatile override var enableContextMenus: Boolean = true

    @Volatile override var requestInterceptor: KromiumRequestInterceptor? = config.requestInterceptor
    @Volatile override var navigationInterceptor: KromiumNavigationInterceptor? = config.navigationInterceptor
    @Volatile override var loadErrorListener: KromiumLoadErrorListener? = config.loadErrorListener
    @Volatile override var authListener: KromiumAuthListener? = config.authListener
    @Volatile override var popupListener: KromiumPopupListener? = config.popupListener
    @Volatile override var jsDialogListener: KromiumJsDialogListener? = config.jsDialogListener
    @Volatile override var contextMenuListener: KromiumContextMenuListener? = config.contextMenuListener

    override fun setContextMenu(block: KromiumMenuBuilder.(KromiumContextMenuContext) -> Unit) {
        contextMenuListener = KromiumContextMenuListener { builder, context ->
            builder.block(context)
        }
    }

    @Volatile override var permissionListener: KromiumPermissionListener? = config.permissionListener
    @Volatile override var consoleMessageListener: KromiumConsoleMessageListener? = config.consoleMessageListener

    internal val requestContext: CefRequestContext

    init {
        requestContext = if (config.isIsolated) {
            CefRequestContext.createContext(null)
        } else {
            CefRequestContext.getGlobalContext()
        }
        
        config.proxy?.let {
            updateProxy(it)
        }
        
        val routerConfig = CefMessageRouter.CefMessageRouterConfig("kromiumQuery", "kromiumCancel")
        val messageRouter = CefMessageRouter.create(routerConfig)
        messageRouter.addHandler(jsHandler, false)
        cefClient.addMessageRouter(messageRouter)
        cefClient.addDownloadHandler(JcefDownloadAdapter(this, downloadManager))
        cefClient.addDisplayHandler(JcefDisplayAdapter(this, config))
        cefClient.addJSDialogHandler(JcefJSDialogAdapter(this, config))
        cefClient.addLifeSpanHandler(JcefLifeSpanAdapter(this, config))
        cefClient.addRequestHandler(JcefRequestAdapter(this, config))
        cefClient.addContextMenuHandler(JcefContextMenuAdapter(this, config))
        cefClient.addPermissionHandler(JcefPermissionAdapter(this, config))
        cefClient.addLoadHandler(JcefLoadAdapter(this, config))
        cefClient.addFocusHandler(JcefFocusAdapter())
        cefClient.addKeyboardHandler(JcefKeyboardAdapter())
    }

    internal val browsers = mutableListOf<JcefKromiumBrowser>()

    override fun createBrowser(url: String, config: KromiumClientConfig): KromiumBrowser {
        val rendering = if (config.isOffScreenRendered || isHeadless) CefRendering.OFFSCREEN else CefRendering.DEFAULT
        val effectiveFramework = config.framework ?: this.config.framework ?: if (isHeadless) KromiumUiFramework.HEADLESS else null
        val effectiveConfig = if (config.framework != effectiveFramework) {
            config.copy(framework = effectiveFramework)
        } else {
            config
        }
        if (rendering == CefRendering.OFFSCREEN && effectiveFramework == null) {
            throw KromiumException.FrameworkRequired()
        }
        val cefBrowser: CefBrowser = if (rendering == CefRendering.OFFSCREEN) {
            CefBrowserOsr(
                cefClient,
                url,
                effectiveConfig.isTransparent,
                requestContext,
                null,
                effectiveConfig
            )
        } else {
            CefBrowserFactory.create(
                cefClient,
                url,
                rendering,
                config.isTransparent,
                requestContext
            )
        }
        val browser = JcefKromiumBrowser(cefBrowser, jsHandler, downloadManager, this)
        browsers.add(browser)
        return browser
    }

    override fun updateProxy(proxy: KromiumProxy): Boolean {
        return try {
            proxy.validate()
            val prefMap = proxy.toPreferenceMap()
            val context = browsers.firstOrNull()?.cefBrowser?.requestContext ?: requestContext
            val error = context.setPreference("proxy", prefMap)
            if (error.isNullOrEmpty()) {
                currentProxy = proxy
                KromiumLogger.i(TAG, "Client proxy updated to: $proxy")
                true
            } else {
                KromiumLogger.e(TAG, "Failed to set client proxy: $error")
                false
            }
        } catch (t: Throwable) {
            KromiumLogger.e(TAG, "Error applying dynamic client proxy", t)
            false
        }
    }

    override fun clearPermissionCache() {
        permissionCache.clear()
    }

    internal fun getPermissionCache(): ConcurrentHashMap<String, Int> = permissionCache

    override fun cancelDownload(downloadId: Int): Boolean = downloadManager.stateManager.cancel(downloadId)
    override fun pauseDownload(downloadId: Int): Boolean = downloadManager.stateManager.pause(downloadId)
    override fun resumeDownload(downloadId: Int): Boolean = downloadManager.stateManager.resume(downloadId)
    override fun isDownloadPaused(downloadId: Int): Boolean = downloadManager.stateManager.isPaused(downloadId)

    internal val loadHandlers = CopyOnWriteArrayList<CefLoadHandler>()

    internal fun addLoadHandler(handler: CefLoadHandler) {
        loadHandlers.add(handler)
    }

    internal fun removeLoadHandler(handler: CefLoadHandler) {
        loadHandlers.remove(handler)
    }

    override fun dispose() {
        engine.activeClients.remove(this)
        loadHandlers.clear()
        browsers.forEach { it.close() }
        browsers.clear()
        permissionCache.clear()
        cefClient.dispose()
    }
}
