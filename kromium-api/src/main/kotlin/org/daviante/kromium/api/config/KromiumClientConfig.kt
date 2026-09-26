package org.daviante.kromium.api.config

import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.api.download.KromiumDownloadListener
import org.daviante.kromium.api.download.KromiumBeforeDownloadListener
import org.daviante.kromium.api.network.KromiumRequestInterceptor
import org.daviante.kromium.api.network.KromiumNavigationInterceptor
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumPopupListener
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.SslErrorPolicy
import org.daviante.kromium.api.ui.KromiumContextMenuListener
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.permission.KromiumPermissionListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.api.navigation.KromiumLoadErrorListener
import org.daviante.kromium.api.proxy.KromiumProxy

/**
 * Configuration for a specific browser client session.
 * This determines proxy settings, user agents, and attaches listeners for network and browser events.
 */
data class KromiumClientConfig(
    val userAgent: String? = null,
    val emulateDesktopEnvironment: Boolean = false,
    val doNotTrack: Boolean = true,
    val isIsolated: Boolean = false,
    val proxy: KromiumProxy? = null,
    
    val onBeforeDownloadListener: KromiumBeforeDownloadListener? = null,
    val downloadListener: KromiumDownloadListener? = null,
    val requestInterceptor: KromiumRequestInterceptor? = null,
    val navigationInterceptor: KromiumNavigationInterceptor? = null,
    val consoleMessageListener: KromiumConsoleMessageListener? = null,
    val jsDialogListener: KromiumJsDialogListener? = null,
    val popupListener: KromiumPopupListener? = null,
    val authListener: KromiumAuthListener? = null,
    val contextMenuListener: KromiumContextMenuListener? = null,
    val permissionListener: KromiumPermissionListener? = null,
    val loadErrorListener: KromiumLoadErrorListener? = null,
    val isOffScreenRendered: Boolean = true,
    val isTransparent: Boolean = false,
    val framework: KromiumUiFramework? = null,
    val sslErrorPolicy: SslErrorPolicy = SslErrorPolicy.Strict,
    val initialWidth: Int = 1280,
    val initialHeight: Int = 800
) {
    companion object {
        @JvmStatic
        fun default(): KromiumClientConfig = KromiumClientConfig()

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var userAgent: String? = null
        private var emulateDesktopEnvironment: Boolean = false
        private var doNotTrack: Boolean = true
        private var isIsolated: Boolean = false
        private var proxy: KromiumProxy? = null
        
        private var onBeforeDownloadListener: KromiumBeforeDownloadListener? = null
        private var downloadListener: KromiumDownloadListener? = null
        private var requestInterceptor: KromiumRequestInterceptor? = null
        private var navigationInterceptor: KromiumNavigationInterceptor? = null
        private var consoleMessageListener: KromiumConsoleMessageListener? = null
        private var jsDialogListener: KromiumJsDialogListener? = null
        private var popupListener: KromiumPopupListener? = null
        private var authListener: KromiumAuthListener? = null
        private var contextMenuListener: KromiumContextMenuListener? = null
        private var permissionListener: KromiumPermissionListener? = null
        private var loadErrorListener: KromiumLoadErrorListener? = null
        private var isOffScreenRendered: Boolean = true
        private var isTransparent: Boolean = false
        private var framework: KromiumUiFramework? = null
        private var sslErrorPolicy: SslErrorPolicy = SslErrorPolicy.Strict
        private var initialWidth: Int = 1280
        private var initialHeight: Int = 800

        fun userAgent(userAgent: String?) = apply { this.userAgent = userAgent }
        fun emulateDesktopEnvironment(emulate: Boolean) = apply { this.emulateDesktopEnvironment = emulate }
        fun doNotTrack(doNotTrack: Boolean) = apply { this.doNotTrack = doNotTrack }
        fun isolated(isolated: Boolean = true) = apply { this.isIsolated = isolated }
        fun proxy(proxy: KromiumProxy?) = apply { this.proxy = proxy }
        fun offScreenRendered(offScreen: Boolean) = apply { this.isOffScreenRendered = offScreen }
        fun transparent(transparent: Boolean) = apply { this.isTransparent = transparent }
        fun framework(framework: KromiumUiFramework?) = apply { this.framework = framework }
        fun framework(frameworkName: String?) = apply { this.framework = frameworkName?.let { KromiumUiFramework.fromString(it) } }

        fun onBeforeDownload(listener: KromiumBeforeDownloadListener) = apply { this.onBeforeDownloadListener = listener }
        fun downloadListener(listener: KromiumDownloadListener) = apply { this.downloadListener = listener }
        fun requestInterceptor(interceptor: KromiumRequestInterceptor) = apply { this.requestInterceptor = interceptor }
        fun navigationInterceptor(interceptor: KromiumNavigationInterceptor) = apply { this.navigationInterceptor = interceptor }
        fun consoleMessageListener(listener: KromiumConsoleMessageListener) = apply { this.consoleMessageListener = listener }
        fun jsDialogListener(listener: KromiumJsDialogListener) = apply { this.jsDialogListener = listener }
        fun popupListener(listener: KromiumPopupListener) = apply { this.popupListener = listener }
        fun authListener(listener: KromiumAuthListener) = apply { this.authListener = listener }
        fun contextMenuListener(listener: KromiumContextMenuListener) = apply { this.contextMenuListener = listener }
        fun contextMenu(block: KromiumMenuBuilder.(KromiumContextMenuContext) -> Unit) = apply {
            this.contextMenuListener = KromiumContextMenuListener { b, c -> b.block(c) }
        }
        fun permissionListener(listener: KromiumPermissionListener) = apply { this.permissionListener = listener }
        fun loadErrorListener(listener: KromiumLoadErrorListener) = apply { this.loadErrorListener = listener }
        fun sslErrorPolicy(policy: SslErrorPolicy) = apply { this.sslErrorPolicy = policy }
        fun initialWidth(width: Int) = apply { this.initialWidth = width }
        fun initialHeight(height: Int) = apply { this.initialHeight = height }

        fun build(): KromiumClientConfig {
            return KromiumClientConfig(
                userAgent = userAgent,
                emulateDesktopEnvironment = emulateDesktopEnvironment,
                doNotTrack = doNotTrack,
                isIsolated = isIsolated,
                proxy = proxy,
                onBeforeDownloadListener = onBeforeDownloadListener,
                downloadListener = downloadListener,
                requestInterceptor = requestInterceptor,
                navigationInterceptor = navigationInterceptor,
                consoleMessageListener = consoleMessageListener,
                jsDialogListener = jsDialogListener,
                popupListener = popupListener,
                authListener = authListener,
                contextMenuListener = contextMenuListener,
                permissionListener = permissionListener,
                loadErrorListener = loadErrorListener,
                isOffScreenRendered = isOffScreenRendered,
                isTransparent = isTransparent,
                framework = framework,
                sslErrorPolicy = sslErrorPolicy,
                initialWidth = initialWidth,
                initialHeight = initialHeight
            )
        }
    }
}
