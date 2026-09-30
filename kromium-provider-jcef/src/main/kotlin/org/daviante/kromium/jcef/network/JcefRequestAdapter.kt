package org.daviante.kromium.jcef.network

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.KromiumAuthRequest
import org.daviante.kromium.api.network.KromiumAuthResponse
import org.daviante.kromium.api.network.KromiumWebResourceRequest
import org.daviante.kromium.api.network.KromiumRequestInterceptor
import org.daviante.kromium.api.network.SslErrorPolicy
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.api.network.KromiumAssetFilter
import org.daviante.kromium.jcef.core.JcefKromiumBrowser
import org.daviante.kromium.jcef.core.JcefKromiumClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefAuthCallback
import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.cef.network.CefURLRequest
import org.cef.security.CefSSLInfo

internal class JcefRequestAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefRequestHandlerAdapter() {

    companion object {
        private const val TAG = "JcefRequestAdapter"
    }

    private fun getKromiumBrowser(cefBrowser: CefBrowser?): JcefKromiumBrowser? {
        if (cefBrowser == null) return null
        return client.browsers.find { it.cefBrowser == cefBrowser }
    }

    override fun onBeforeBrowse(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        userGesture: Boolean,
        isRedirect: Boolean
    ): Boolean {
        val targetUrl = request?.url ?: return false

        val interceptor = client.navigationInterceptor ?: config.navigationInterceptor
        if (interceptor != null) {
            val shouldOverride = try {
                interceptor.shouldOverrideUrlLoading(targetUrl, isRedirect)
            } catch (e: Throwable) {
                KromiumLogger.e(TAG, "navigationInterceptor threw an exception for $targetUrl", e)
                false
            }
            if (shouldOverride) {
                KromiumLogger.d(TAG, "Navigation intercepted and overridden: $targetUrl")
                return true
            }
        }
        
        return false
    }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?
    ): CefResourceRequestHandler? {
        val kromiumBrowser = getKromiumBrowser(browser)

        return object : CefResourceRequestHandlerAdapter() {
            override fun onBeforeResourceLoad(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?
            ): Boolean {
                if (request == null) return false

                var webReq: KromiumWebResourceRequest? = null
                fun getOrCreateWebReq(): KromiumWebResourceRequest {
                    var r = webReq
                    if (r == null) {
                        val headersMap = mutableMapOf<String, String>()
                        request.getHeaderMap(headersMap)
                        r = KromiumWebResourceRequest(
                            url = request.url ?: "",
                            method = request.method ?: "GET",
                            headers = headersMap,
                            isNavigation = (frame?.isMain == true),
                            isDownload = isDownload,
                            resourceType = JcefResourceTypeMapper.map(request.resourceType),
                            requestInitiator = requestInitiator
                        )
                        webReq = r
                    }
                    return r
                }

                // Asset Blocking
                if (kromiumBrowser != null) {
                    val assets = kromiumBrowser.assets as JcefAssets
                    val filter = assets.filter
                    if (filter != null) {
                        if (filter.shouldBlock(getOrCreateWebReq())) {
                            return true
                        }
                    }

                    // Security / HostLock
                    val security = kromiumBrowser.security as JcefSecurity
                    val allowed = security.hostLock
                    val isMainFrame = frame?.isMain ?: false

                    if (!allowed.isNullOrEmpty()) {
                        val targetUrl = request.url

                        // Check navigation locks
                        if (isNavigation && (isMainFrame || security.hostLockSubframes)) {
                            if (!KromiumAssetFilter.isHostAllowed(targetUrl, allowed)) {
                                KromiumLogger.w(TAG, "Navigation blocked by host lock: $targetUrl")
                                return true
                            }
                        }

                        // Check subresource locks
                        if (!isNavigation && security.hostLockSubresources) {
                            if (!KromiumAssetFilter.isHostAllowed(targetUrl, allowed)) {
                                KromiumLogger.w(TAG, "Subresource blocked by host lock: $targetUrl")
                                return true
                            }
                        }
                    }
                }

                // Custom User-Agent injection
                config.userAgent?.let { ua ->
                    request.setHeaderByName("User-Agent", ua, true)
                }

                // Do Not Track & Sec-GPC
                if (client.doNotTrack) {
                    request.setHeaderByName("DNT", "1", true)
                    request.setHeaderByName("Sec-GPC", "1", true)
                }

                // Call request interceptor if available
                val interceptor = client.requestInterceptor ?: config.requestInterceptor
                if (interceptor != null) {
                    val req = getOrCreateWebReq()

                    val shouldBlock = try {
                        interceptor.intercept(req)
                    } catch (e: Throwable) {
                        KromiumLogger.e(TAG, "Request interceptor threw an exception for ${request.url}", e)
                        false
                    }

                    if (shouldBlock) {
                        KromiumLogger.d(TAG, "Request blocked by interceptor: ${request.url}")
                        return true
                    }

                    req.headers.forEach { (key, value) ->
                        request.setHeaderByName(key, value, true)
                    }
                }

                // Synthetic in-memory HTML payloads are resolved locally via getResourceHandler and do not fire onResourceLoadComplete
                val isSyntheticPayload = request.url?.let { client.htmlPayloads.containsKey(it) } == true
                if (!isSyntheticPayload) {
                    client.trackRequestStart(request.identifier)
                }
                
                return false
            }

            override fun onResourceLoadComplete(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
                response: CefResponse?,
                status: CefURLRequest.Status?,
                receivedContentLength: Long
            ) {
                if (request != null) {
                    client.trackRequestEnd(request.identifier)
                }
            }

            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?
            ): CefResourceHandler? {
                val url = request?.url ?: return null
                val payload = client.htmlPayloads[url]
                if (payload != null) {
                    return JcefHtmlResourceHandler(payload)
                }
                return null
            }
        }
    }

    override fun getAuthCredentials(
        browser: CefBrowser?,
        origin_url: String?,
        isProxy: Boolean,
        host: String?,
        port: Int,
        realm: String?,
        scheme: String?,
        callback: CefAuthCallback?
    ): Boolean {
        val listener = client.authListener ?: config.authListener
        if (listener != null) {
            val req = KromiumAuthRequest(
                isProxy = isProxy,
                host = host ?: "",
                port = port,
                realm = realm ?: "",
                scheme = scheme ?: ""
            )

            val response = try {
                listener.onAuthRequired(req)
            } catch (e: Throwable) {
                KromiumLogger.e(TAG, "Auth listener threw an exception", e)
                KromiumAuthResponse.Cancel
            }

            return when (response) {
                is KromiumAuthResponse.Proceed -> {
                    callback?.Continue(response.username, response.password)
                    true
                }
                is KromiumAuthResponse.Cancel -> {
                    callback?.cancel()
                    false
                }
            }
        }

        if (isProxy) {
            val creds = client.currentProxy?.getCredentials(host, port)
            if (creds != null) {
                KromiumLogger.d(TAG, "Supplying configured proxy credentials for $host:$port (user: ${creds.first})")
                callback?.Continue(creds.first, creds.second)
                return true
            }
        }

        callback?.cancel()
        return false
    }

    override fun onCertificateError(
        browser: CefBrowser?,
        certError: CefLoadHandler.ErrorCode?,
        requestUrl: String?,
        sslInfo: CefSSLInfo?,
        callback: CefCallback?
    ): Boolean {
        val policy = config.sslErrorPolicy
        val shouldAllow = when (policy) {
            is SslErrorPolicy.Strict -> false
            is SslErrorPolicy.AllowAll -> true
            is SslErrorPolicy.AllowDomains -> policy.isAllowed(requestUrl)
        }

        if (shouldAllow) {
            KromiumLogger.w(TAG, "SSL certificate error bypassed for: $requestUrl (error: $certError)")
            callback?.Continue()
            return true
        }

        KromiumLogger.d(TAG, "SSL certificate error rejected for: $requestUrl (error: $certError)")
        callback?.cancel()
        return false
    }
}
