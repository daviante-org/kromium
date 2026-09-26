package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.automation.KromiumAutomation
import org.daviante.kromium.api.automation.KromiumClipboard
import org.daviante.kromium.api.automation.KromiumJsBridge
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.devtools.KromiumDevTools
import org.daviante.kromium.api.download.KromiumDownloads
import org.daviante.kromium.api.navigation.KromiumNavigation
import org.daviante.kromium.api.navigation.KromiumNavigationState
import org.daviante.kromium.api.navigation.NavigationStage
import org.daviante.kromium.api.network.KromiumAssets
import org.daviante.kromium.api.network.KromiumSecurity
import org.daviante.kromium.api.print.KromiumPdfSettings
import org.daviante.kromium.api.proxy.KromiumProxyManager
import org.daviante.kromium.api.search.KromiumSearch
import org.daviante.kromium.api.storage.KromiumStorage
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumRenderSurface
import org.daviante.kromium.api.ui.KromiumView
import org.daviante.kromium.core.download.KromiumDownloadManager
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.core.util.KromiumFutureBridge
import org.daviante.kromium.jcef.ui.JcefKeyboardDispatcher
import org.daviante.kromium.jcef.automation.JcefAutomationEngine
import org.daviante.kromium.jcef.automation.JcefJsEvaluator
import org.daviante.kromium.jcef.automation.JcefJsHandler
import org.daviante.kromium.jcef.network.JcefAssets
import org.daviante.kromium.jcef.network.JcefSecurity
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.print.JcefPdfSettingsMapper
import org.daviante.kromium.jcef.proxy.JcefProxyManager
import org.daviante.kromium.jcef.search.JcefSearchEngine
import org.daviante.kromium.jcef.storage.JcefStorageEngine
import org.daviante.kromium.jcef.ui.JcefRenderSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import org.cef.browser.CefFrame
import org.cef.callback.CefPdfPrintCallback
import org.cef.callback.CefStringVisitor
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.KEY_TEXT_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_OFF
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
import java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
import java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * JCEF implementation of the KromiumBrowser interfaces.
 */
internal class JcefKromiumBrowser(
    internal val cefBrowser: CefBrowser,
    private val jsHandler: JcefJsHandler,
    private val downloadManager: KromiumDownloadManager,
    private val client: JcefKromiumClient
) : KromiumBrowser,
    KromiumNavigation,
    KromiumView,
    KromiumDevTools,
    KromiumJsBridge,
    KromiumClipboard,
    KromiumDownloads {

    override val navigation: KromiumNavigation get() = this
    override val view: KromiumView get() = this
    override val search: KromiumSearch = JcefSearchEngine(cefBrowser)
    override val devTools: KromiumDevTools get() = this
    override val jsBridge: KromiumJsBridge get() = this
    override val clipboard: KromiumClipboard get() = this
    override val downloads: KromiumDownloads get() = this
    override val automation = JcefAutomationEngine(this, cefBrowser)
    override val storage = JcefStorageEngine(this, cefBrowser)
    override val proxyManager = JcefProxyManager(cefBrowser, client)
    override val security = JcefSecurity()
    override val assets = JcefAssets()

    private val _navigationState = MutableStateFlow(KromiumNavigationState())
    override val navigationState: StateFlow<KromiumNavigationState> = _navigationState.asStateFlow()

    internal fun updateNavigationState(isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean, url: String? = null) {
        val currentUrl = url?.takeIf { it.isNotBlank() } ?: cefBrowser.url ?: _navigationState.value.url
        _navigationState.value = KromiumNavigationState(isLoading, canGoBack, canGoForward, currentUrl, _navigationState.value.title)
    }

    internal fun updateTitle(title: String) {
        if (title != _navigationState.value.title) {
            _navigationState.value = _navigationState.value.copy(title = title)
        }
    }

    internal fun updateLoading(isLoading: Boolean, url: String? = null) {
        val currentUrl = url?.takeIf { it.isNotBlank() } ?: cefBrowser.url ?: _navigationState.value.url
        _navigationState.value = _navigationState.value.copy(isLoading = isLoading, url = currentUrl)
    }

    internal fun updateUrl(url: String) {
        if (url.isNotBlank() && _navigationState.value.url != url) {
            _navigationState.value = _navigationState.value.copy(url = url)
        }
    }

    override suspend fun waitForNetworkIdle(idleTimeMs: Long, maxTimeoutMs: Long, pollIntervalMs: Long): Boolean {
        val startNs = System.nanoTime()
        return withTimeoutOrNull(maxTimeoutMs) {
            while (isActive) {
                if (client.inFlightRequestCount == 0 && client.timeSinceLastRequestMs() >= idleTimeMs) {
                    return@withTimeoutOrNull true
                }
                delay(pollIntervalMs)
            }
            false
        } ?: false
    }

    override fun waitForNetworkIdleAsync(idleTimeMs: Long, maxTimeoutMs: Long, pollIntervalMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { waitForNetworkIdle(idleTimeMs, maxTimeoutMs, pollIntervalMs) }

    private suspend fun internalWaitForNavigation(stage: NavigationStage, timeoutMs: Long, trigger: (() -> Unit)? = null): Boolean {
        val startNs = System.nanoTime()
        val completed = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val handler = object : CefLoadHandlerAdapter() {
                    override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {
                        if (frame?.isMain == true && (browser == null || browser.identifier == cefBrowser.identifier)) {
                            if (stage == NavigationStage.STARTED && continuation.isActive) {
                                client.removeLoadHandler(this)
                                continuation.resume(true)
                            }
                        }
                    }

                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        if (frame?.isMain == true && (browser == null || browser.identifier == cefBrowser.identifier)) {
                            if ((stage == NavigationStage.LOADED || stage == NavigationStage.NETWORK_IDLE) && continuation.isActive) {
                                client.removeLoadHandler(this)
                                continuation.resume(true)
                            }
                        }
                    }

                    override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                        if (frame?.isMain == true && (browser == null || browser.identifier == cefBrowser.identifier)) {
                            if (errorCode != CefLoadHandler.ErrorCode.ERR_ABORTED && continuation.isActive) {
                                client.removeLoadHandler(this)
                                continuation.resume(false)
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    client.removeLoadHandler(handler)
                }

                client.addLoadHandler(handler)
                trigger?.invoke()
            }
        } ?: false

        if (!completed) {
            KromiumLogger.w("JcefKromiumBrowser", "waitForNavigation($stage) timed out after ${timeoutMs}ms")
            return false
        }

        if (stage == NavigationStage.NETWORK_IDLE) {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            val remainingMs = (timeoutMs - elapsedMs).coerceAtLeast(100L)
            return waitForNetworkIdle(idleTimeMs = 500L, maxTimeoutMs = remainingMs)
        }

        return true
    }

    override suspend fun waitForNavigation(stage: NavigationStage, timeoutMs: Long): Boolean =
        internalWaitForNavigation(stage, timeoutMs, null)

    override fun waitForNavigationAsync(stage: NavigationStage, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { waitForNavigation(stage, timeoutMs) }

    override suspend fun loadUrl(url: String, waitUntil: NavigationStage, timeoutMs: Long): Boolean =
        internalWaitForNavigation(waitUntil, timeoutMs) { cefBrowser.loadURL(url) }

    override fun loadUrlAsync(url: String, waitUntil: NavigationStage, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { loadUrl(url, waitUntil, timeoutMs) }

    override fun loadUrl(url: String) {
        cefBrowser.loadURL(url)
    }

    override fun loadHtml(html: String, url: String) {
        val id = UUID.randomUUID().toString()
        val separator = if (url.endsWith("/")) "" else "/"
        val targetUrl = "$url$separator$id"
        client.registerHtmlPayload(targetUrl, html)
        cefBrowser.loadURL(targetUrl)
    }

    override val surface = JcefRenderSurface(cefBrowser)

    override fun executeJavaScript(code: String) {
        cefBrowser.executeJavaScript(code, cefBrowser.url, 0)
    }

    override var zoomLevel: Double
        get() = cefBrowser.zoomLevel
        set(value) { cefBrowser.zoomLevel = value }

    override fun setZoom(level: Double) {
        cefBrowser.zoomLevel = level
    }

    override fun getZoom(): Double = cefBrowser.zoomLevel
    
    override fun setAntialiasing(enabled: Boolean) {
        val osrPanel = cefBrowser.uiComponent as? JcefOsrComponent ?: (cefBrowser as? CefBrowserOsr)?.osrComponent ?: return
        val hint = if (enabled) VALUE_ANTIALIAS_ON else VALUE_ANTIALIAS_OFF
        osrPanel.setRenderingHint(KEY_ANTIALIASING, hint)
        osrPanel.setRenderingHint(KEY_TEXT_ANTIALIASING, hint)
    }

    override fun setInterpolation(quality: String) {
        val osrPanel = cefBrowser.uiComponent as? JcefOsrComponent ?: (cefBrowser as? CefBrowserOsr)?.osrComponent ?: return
        val hint = when(quality.lowercase()) {
            "high", "bicubic" -> VALUE_INTERPOLATION_BICUBIC
            "bilinear" -> VALUE_INTERPOLATION_BILINEAR
            else -> VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        }
        osrPanel.setInterpolation(hint)
    }

    override var scaleFactor: Double
        get() = (cefBrowser as? CefBrowserOsr)?.scaleFactor ?: 1.0
        set(value) {
            (cefBrowser as? CefBrowserOsr)?.scaleFactor = value
        }

    override var isAutoDetectScaleFactor: Boolean
        get() = (cefBrowser as? CefBrowserOsr)?.isAutoDetectScaleFactor ?: false
        set(value) {
            (cefBrowser as? CefBrowserOsr)?.isAutoDetectScaleFactor = value
        }

    override var scrollMultiplier: Double
        get() = (cefBrowser as? CefBrowserOsr)?.scrollMultiplier ?: 1.0
        set(value) {
            (cefBrowser as? CefBrowserOsr)?.scrollMultiplier = value
        }

    override fun resetScaleFactorToAuto() {
        (cefBrowser as? CefBrowserOsr)?.isAutoDetectScaleFactor = true
    }

    override fun goBack() {
        cefBrowser.goBack()
    }

    override fun goForward() {
        cefBrowser.goForward()
    }

    override fun canGoBack(): Boolean = cefBrowser.canGoBack()

    override fun canGoForward(): Boolean = cefBrowser.canGoForward()

    override fun reload(ignoreCache: Boolean) {
        if (ignoreCache) {
            cefBrowser.reloadIgnoreCache()
        } else {
            cefBrowser.reload()
        }
    }

    override fun stopLoad() {
        cefBrowser.stopLoad()
    }

    override fun openDevTools(inspectPoint: Point?) {
        if (inspectPoint != null) {
            cefBrowser.openDevTools(inspectPoint)
        } else {
            cefBrowser.openDevTools()
        }
    }

    override fun closeDevTools() {
        cefBrowser.closeDevTools()
    }

    private val activeFrame get() = cefBrowser.focusedFrame ?: cefBrowser.mainFrame

    override fun copy() { activeFrame?.copy() }
    override fun paste() { activeFrame?.paste() }
    override fun cut() { activeFrame?.cut() }
    override fun selectAll() { activeFrame?.selectAll() }
    override fun undo() { activeFrame?.undo() }
    override fun redo() { activeFrame?.redo() }

    override fun startDownload(url: String) {
        cefBrowser.startDownload(url)
    }

    override fun close() {
        cefBrowser.close(true)
    }

    override fun takeScreenshot(): BufferedImage? {
        val component = cefBrowser.uiComponent ?: return null
        if (component.width <= 0 || component.height <= 0) return null

        if (component.isShowing) {
            try {
                val loc = component.locationOnScreen
                if (loc.x >= 0 && loc.y >= 0) {
                    val rect = Rectangle(loc.x, loc.y, component.width, component.height)
                    return Robot().createScreenCapture(rect)
                }
            } catch (_: Throwable) {
                // Fallback
            }
        }

        val image = BufferedImage(
            component.width,
            component.height,
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics = image.createGraphics()
        component.paint(graphics)
        graphics.dispose()
        return image
    }

    override fun takeScreenshotAsync(): CompletableFuture<BufferedImage?> {
        val future = CompletableFuture<BufferedImage?>()
        cefBrowser.createScreenshot(true).whenComplete { img, ex ->
            if (ex != null) {
                future.completeExceptionally(ex)
            } else {
                future.complete(img)
            }
        }
        return future
    }

    override fun print() {
        cefBrowser.print()
    }

    override fun printToPdf(path: String, settings: KromiumPdfSettings): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val cefSettings = JcefPdfSettingsMapper.map(settings)
        cefBrowser.printToPDF(path, cefSettings, object : CefPdfPrintCallback {
            override fun onPdfPrintFinished(path: String?, ok: Boolean) {
                future.complete(ok)
            }
        })
        return future
    }

    override fun sendKeyEvent(event: KromiumKeyEvent) {
        JcefKeyboardDispatcher.dispatch(cefBrowser, event)
    }

    override fun setFocus(focused: Boolean) {
        cefBrowser.setFocus(focused)
    }

    override fun resize(width: Int, height: Int) {
        if (cefBrowser is CefBrowserOsr) {
            (cefBrowser as CefBrowserOsr).notifyResized(width, height)
        } else {
            cefBrowser.wasResized(width, height)
        }
    }

    override suspend fun evaluateJavaScript(
        expression: String,
        timeoutMs: Long?,
        bindingTimeoutMs: Long?,
        bindingIntervalMs: Long?
    ): String? {
        return JcefJsEvaluator.evaluate(
            browser = cefBrowser,
            handler = jsHandler,
            expression = expression,
            routerQueryName = "kromiumQuery",
            timeoutMs = timeoutMs,
            throwOnTimeout = false,
            bindingTimeoutMs = bindingTimeoutMs,
            bindingIntervalMs = bindingIntervalMs
        )
    }

    override suspend fun getHtml(): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        cefBrowser.getSource(object : CefStringVisitor {
            override fun visit(string: String?) {
                if (cont.isActive) {
                    cont.resume(string ?: "")
                }
            }
        })
    }

    override fun getHtmlAsync(): CompletableFuture<String> =
        KromiumFutureBridge.toCompletableFuture { getHtml() }

    override suspend fun getText(): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        cefBrowser.getText(object : CefStringVisitor {
            override fun visit(string: String?) {
                if (cont.isActive) {
                    cont.resume(string ?: "")
                }
            }
        })
    }

    override fun getTextAsync(): CompletableFuture<String> =
        KromiumFutureBridge.toCompletableFuture { getText() }

    override suspend fun getFaviconUrl(): String? {
        val script = """
            (function() {
                var link = document.querySelector("link[rel*='icon']");
                return link ? link.href : '';
            })();
        """.trimIndent()
        val url = evaluateJavaScript(script)
        return if (url.isNullOrBlank()) null else url
    }

    override fun getFaviconUrlAsync(): CompletableFuture<String?> =
        KromiumFutureBridge.toCompletableFuture { getFaviconUrl() }

    // --- KromiumDownloads Implementation ---

    override fun cancelDownload(downloadId: Int): Boolean {
        return downloadManager.stateManager.cancel(downloadId)
    }

    override fun pauseDownload(downloadId: Int): Boolean {
        return downloadManager.stateManager.pause(downloadId)
    }

    override fun resumeDownload(downloadId: Int): Boolean {
        return downloadManager.stateManager.resume(downloadId)
    }

    override fun isDownloadPaused(downloadId: Int): Boolean {
        return downloadManager.stateManager.isPaused(downloadId)
    }
}
