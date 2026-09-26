package org.daviante.kromium.jcef.cookie

import org.daviante.kromium.api.cookie.KromiumCookie
import org.daviante.kromium.api.cookie.KromiumCookieManager
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.core.util.KromiumFutureBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.callback.CefCookieVisitor
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.net.URI
import java.util.Date
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume

private const val TAG = "JcefCookieManager"

/**
 * JCEF implementation of the KromiumCookieManager.
 */
class JcefCookieManager : KromiumCookieManager {

    @Volatile override var timeoutMs: Long = 2_000L

    private val rawManager: CefCookieManager
        get() = CefCookieManager.getGlobalManager()

    override suspend fun getCookies(url: String, includeHttpOnly: Boolean): Map<String, String> {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.equals("about:blank", ignoreCase = true) || !trimmed.startsWith("http", ignoreCase = true)) {
            return emptyMap()
        }

        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val cookies = mutableMapOf<String, String>()
                val manager = try {
                    rawManager
                } catch (e: Throwable) {
                    KromiumLogger.w(TAG, "Could not access global cookie manager", e)
                    null
                }

                if (manager == null) {
                    continuation.resume(emptyMap())
                    return@suspendCancellableCoroutine
                }

                val success = try {
                    manager.visitUrlCookies(url, includeHttpOnly, object : CefCookieVisitor {
                        override fun visit(
                            cookie: CefCookie?,
                            count: Int,
                            total: Int,
                            deleteCookie: BoolRef?
                        ): Boolean {
                            if (cookie != null && cookie.name.isNotBlank()) {
                                cookies[cookie.name] = cookie.value
                            }
                            if (total == 0 || count >= total - 1) {
                                if (continuation.isActive) {
                                    continuation.resume(cookies)
                                }
                            }
                            return true
                        }
                    })
                } catch (e: Throwable) {
                    KromiumLogger.w(TAG, "Failed to visit URL cookies for $url", e)
                    false
                }

                if (!success && continuation.isActive) {
                    continuation.resume(emptyMap())
                }
            }
        }

        if (result == null) {
            KromiumLogger.d(TAG, "Cookie retrieval timed out for $url (${timeoutMs}ms), returning empty map")
        }

        return result ?: emptyMap()
    }

    override fun getCookiesAsync(url: String, includeHttpOnly: Boolean): CompletableFuture<Map<String, String>> =
        KromiumFutureBridge.toCompletableFuture { getCookies(url, includeHttpOnly) }

    override suspend fun getCookie(url: String, name: String): String? {
        return getCookies(url)[name]
    }

    override fun getCookieAsync(url: String, name: String): CompletableFuture<String?> =
        KromiumFutureBridge.toCompletableFuture { getCookie(url, name) }

    override suspend fun getAllCookies(): List<KromiumCookie> {
        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val cookies = mutableListOf<KromiumCookie>()
                val manager = try {
                    rawManager
                } catch (e: Throwable) {
                    KromiumLogger.w(TAG, "Could not access global cookie manager", e)
                    null
                }

                if (manager == null) {
                    continuation.resume(emptyList())
                    return@suspendCancellableCoroutine
                }

                val success = try {
                    manager.visitAllCookies(object : CefCookieVisitor {
                        override fun visit(
                            cookie: CefCookie?,
                            count: Int,
                            total: Int,
                            deleteCookie: BoolRef?
                        ): Boolean {
                            if (cookie != null) {
                                cookies.add(KromiumCookie(
                                    name = cookie.name,
                                    value = cookie.value,
                                    domain = cookie.domain,
                                    path = cookie.path,
                                    isSecure = cookie.secure,
                                    isHttpOnly = cookie.httponly,
                                    hasExpires = cookie.hasExpires,
                                    expires = cookie.expires
                                ))
                            }
                            if (total == 0 || count == total - 1) {
                                if (continuation.isActive) {
                                    continuation.resume(cookies)
                                }
                            }
                            return true
                        }
                    })
                } catch (e: Throwable) {
                    KromiumLogger.w(TAG, "Failed to visit all cookies", e)
                    false
                }

                if (!success && continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        }

        if (result == null) {
            KromiumLogger.d(TAG, "All-cookies retrieval timed out (${timeoutMs}ms), returning empty list")
        }

        return result ?: emptyList()
    }

    override fun getAllCookiesAsync(): CompletableFuture<List<KromiumCookie>> =
        KromiumFutureBridge.toCompletableFuture { getAllCookies() }

    override fun setCookie(
        url: String,
        name: String,
        value: String,
        domain: String?,
        path: String,
        isSecure: Boolean,
        isHttpOnly: Boolean,
        expires: Date?
    ): Boolean {
        val resolvedDomain = domain ?: try {
            URI(url).host
        } catch (e: Throwable) {
            KromiumLogger.w(TAG, "Could not parse domain from URL: $url", e)
            null
        }

        val cookie = CefCookie(
            name,
            value,
            resolvedDomain,
            path,
            isSecure,
            isHttpOnly,
            null,
            null,
            expires != null,
            expires
        )
        return try {
            rawManager.setCookie(url, cookie)
        } catch (e: Throwable) {
            KromiumLogger.w(TAG, "Failed to set cookie '$name' for $url", e)
            false
        }
    }

    override fun deleteCookie(url: String, name: String): Boolean {
        return try {
            rawManager.deleteCookies(url, name)
        } catch (e: Throwable) {
            KromiumLogger.w(TAG, "Failed to delete cookie '$name' for $url", e)
            false
        }
    }

    override fun clearCookies(): Boolean {
        return try {
            rawManager.deleteCookies("", "")
        } catch (e: Throwable) {
            KromiumLogger.w(TAG, "Failed to clear all cookies", e)
            false
        }
    }

    override fun flush(): Boolean {
        return try {
            rawManager.flushStore(null)
        } catch (e: Throwable) {
            KromiumLogger.w(TAG, "Failed to flush cookie store", e)
            false
        }
    }
}
