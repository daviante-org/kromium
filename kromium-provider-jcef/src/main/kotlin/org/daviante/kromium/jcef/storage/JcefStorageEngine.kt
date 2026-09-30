package org.daviante.kromium.jcef.storage

import org.daviante.kromium.api.storage.KromiumStorage
import org.daviante.kromium.api.automation.KromiumJsBridge
import org.daviante.kromium.core.util.KromiumFutureBridge
import org.cef.browser.CefBrowser
import org.cef.network.CefCookieManager
import org.cef.network.CefCookie
import org.cef.callback.CefCookieVisitor
import java.util.concurrent.CompletableFuture
import java.util.Date
import kotlinx.coroutines.future.await
import java.util.Vector

internal class JcefStorageEngine(
    private val jsBridge: KromiumJsBridge,
    private val cefBrowser: CefBrowser
) : KromiumStorage {

    private val cookieManager: CefCookieManager
        get() = CefCookieManager.getGlobalManager()

    override fun getCookiesAsync(): CompletableFuture<Map<String, String>> = KromiumFutureBridge.toCompletableFuture {
        val url = cefBrowser.url ?: return@toCompletableFuture emptyMap()
        val cookies = mutableMapOf<String, String>()
        val future = CompletableFuture<Map<String, String>>()
        
        cookieManager.visitUrlCookies(url, true, object : CefCookieVisitor {
            override fun visit(cookie: CefCookie?, count: Int, total: Int, delete: org.cef.misc.BoolRef?): Boolean {
                if (cookie != null) {
                    cookies[cookie.name] = cookie.value
                }
                if (count == total - 1) {
                    future.complete(cookies)
                }
                return true
            }
        })
        
        // Timeout or empty case
        Thread {
            Thread.sleep(1000)
            if (!future.isDone) {
                future.complete(cookies)
            }
        }.start()
        
        future.await()
    }

    override fun getCookieAsync(name: String): CompletableFuture<String?> = KromiumFutureBridge.toCompletableFuture {
        val cookies = getCookiesAsync().await()
        cookies[name]
    }

    override fun setCookie(
        name: String,
        value: String,
        domain: String?,
        path: String,
        isSecure: Boolean,
        isHttpOnly: Boolean,
        expires: Date?
    ): Boolean {
        val url = cefBrowser.url ?: return false
        val resolvedDomain = domain ?: try {
            java.net.URI(url).host
        } catch (e: Exception) {
            null
        }

        val cookie = CefCookie(
            name,
            value,
            resolvedDomain,
            path,
            isSecure,
            isHttpOnly,
            Date(), // creation
            Date(), // lastAccess
            expires != null, // hasExpires
            expires // expires
        )
        return cookieManager.setCookie(url, cookie)
    }

    override fun clearCookies(): Boolean {
        return cookieManager.deleteCookies("", "")
    }

    override fun clearWebStorageAsync(): CompletableFuture<Boolean> = KromiumFutureBridge.toCompletableFuture {
        val script = """
            (function() {
                try {
                    window.localStorage.clear();
                    window.sessionStorage.clear();
                    return true;
                } catch(e) {
                    return false;
                }
            })();
        """.trimIndent()
        val res = jsBridge.evaluateJavaScript(script, 5000L)
        res == "true"
    }

    override fun clearBrowsingDataAsync(clearCookies: Boolean, clearStorage: Boolean): CompletableFuture<Boolean> = KromiumFutureBridge.toCompletableFuture {
        var success = true
        if (clearCookies) {
            success = success && clearCookies()
        }
        if (clearStorage) {
            success = success && clearWebStorageAsync().await()
        }
        success
    }
}
