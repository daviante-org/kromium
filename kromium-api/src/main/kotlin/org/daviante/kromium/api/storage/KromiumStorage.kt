package org.daviante.kromium.api.storage

import java.util.concurrent.CompletableFuture
import java.util.Date

/**
 * Facade for Cookie and Storage Management.
 * Provides methods for clearing cache, reading cookies, and mutating storage state.
 */
interface KromiumStorage {
    
    /**
     * Retrieves all cookies available to the current page.
     * Note: This may be limited by HttpOnly flags depending on the engine.
     */
    fun getCookiesAsync(): CompletableFuture<Map<String, String>>
    
    /**
     * Retrieves a specific cookie by name.
     */
    fun getCookieAsync(name: String): CompletableFuture<String?>
    
    /**
     * Sets a cookie on the global cookie manager.
     */
    fun setCookie(
        name: String, 
        value: String, 
        domain: String? = null, 
        path: String = "/", 
        isSecure: Boolean = false, 
        isHttpOnly: Boolean = false, 
        expires: Date? = null
    ): Boolean
    
    /**
     * Clears all cookies from the global manager.
     */
    fun clearCookies(): Boolean
    
    /**
     * Clears Local Storage and Session Storage for the current origin.
     */
    fun clearWebStorageAsync(): CompletableFuture<Boolean>
    
    /**
     * Aggregates cookie and storage clearing.
     */
    fun clearBrowsingDataAsync(clearCookies: Boolean = true, clearStorage: Boolean = true): CompletableFuture<Boolean>
}
