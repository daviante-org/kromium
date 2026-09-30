package org.daviante.kromium.api.cookie

import java.util.Date
import java.util.concurrent.CompletableFuture

/**
 * Interface for managing cookies across the browser engine.
 */
interface KromiumCookieManager {
    /** Timeout for cookie retrieval operations in milliseconds. */
    var timeoutMs: Long

    /**
     * Retrieves all cookies for the specified [url] as a key-value map.
     */
    suspend fun getCookies(url: String, includeHttpOnly: Boolean = true): Map<String, String>

    /**
     * Asynchronously retrieves all cookies for [url] returning a Java [CompletableFuture].
     */
    fun getCookiesAsync(url: String, includeHttpOnly: Boolean = true): CompletableFuture<Map<String, String>>

    /**
     * Retrieves the value of a specific cookie by [name] for [url].
     */
    suspend fun getCookie(url: String, name: String): String?

    /**
     * Asynchronously retrieves a specific cookie by [name] for [url] returning a Java [CompletableFuture].
     */
    fun getCookieAsync(url: String, name: String): CompletableFuture<String?>

    /**
     * Retrieves all cookies across all domains stored in the engine's global cookie store.
     */
    suspend fun getAllCookies(): List<KromiumCookie>

    /**
     * Asynchronously retrieves all cookies across all domains stored in the global cookie store,
     * returning a Java [CompletableFuture].
     */
    fun getAllCookiesAsync(): CompletableFuture<List<KromiumCookie>>

    /**
     * Sets a cookie for the specified [url].
     */
    fun setCookie(
        url: String,
        name: String,
        value: String,
        domain: String? = null,
        path: String = "/",
        isSecure: Boolean = false,
        isHttpOnly: Boolean = false,
        expires: Date? = null
    ): Boolean

    /**
     * Deletes a specific cookie for the specified [url].
     */
    fun deleteCookie(url: String, name: String): Boolean

    /**
     * Deletes all cookies from the cookie store.
     */
    fun clearCookies(): Boolean

    /**
     * Flushes the cookie store to disk to ensure session persistence.
     */
    fun flush(): Boolean
}
