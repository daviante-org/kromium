package org.daviante.kromium.api.automation

import java.util.concurrent.CompletableFuture

/**
 * Facade for DOM automation and browser interaction testing.
 * Provides Playwright-style methods for automating interactions with the loaded page.
 */
interface KromiumAutomation {
    /**
     * Injects JavaScript into the active page to normalize the headless environment and emulate a standard interactive desktop session.
     */
    fun emulateDesktopEnvironment()

    fun simulateClick(x: Int, y: Int)
    
    suspend fun waitForSelector(selector: String, timeoutMs: Long = 10_000L): Boolean
    fun waitForSelectorAsync(selector: String, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>
    
    suspend fun waitForUrl(pattern: String, isRegex: Boolean = false, timeoutMs: Long = 15_000L): Boolean
    fun waitForUrlAsync(pattern: String, isRegex: Boolean = false, timeoutMs: Long = 15_000L): CompletableFuture<Boolean>
    
    suspend fun click(selector: String, timeoutMs: Long = 10_000L): Boolean
    fun clickAsync(selector: String, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>
    
    suspend fun fill(selector: String, value: String, timeoutMs: Long = 10_000L): Boolean
    fun fillAsync(selector: String, value: String, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>
    
    suspend fun type(selector: String, text: String, delayMs: Long = 20L, timeoutMs: Long = 10_000L): Boolean
    fun typeAsync(selector: String, text: String, delayMs: Long = 20L, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>
    
    suspend fun selectOption(selector: String, value: String, timeoutMs: Long = 10_000L): Boolean
    fun selectOptionAsync(selector: String, value: String, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>
    
    suspend fun getTextContent(selector: String, timeoutMs: Long = 10_000L): String?
    fun getTextContentAsync(selector: String, timeoutMs: Long = 10_000L): CompletableFuture<String?>
    
    suspend fun getAttribute(selector: String, attribute: String, timeoutMs: Long = 10_000L): String?
    fun getAttributeAsync(selector: String, attribute: String, timeoutMs: Long = 10_000L): CompletableFuture<String?>
    
    suspend fun isVisible(selector: String): Boolean
    fun isVisibleAsync(selector: String): CompletableFuture<Boolean>
    
    suspend fun isChecked(selector: String): Boolean
    fun isCheckedAsync(selector: String): CompletableFuture<Boolean>
    
    suspend fun count(selector: String): Int
    fun countAsync(selector: String): CompletableFuture<Int>
}
