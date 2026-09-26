package org.daviante.kromium.api.navigation

import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CompletableFuture

/**
 * Facade for browser navigation operations.
 */
interface KromiumNavigation {
    /**
     * Loads a specific URL in the browser and suspends until the navigation reaches the requested stage.
     * @param url The URL to load.
     * @param waitUntil The lifecycle stage to await (default: LOADED).
     * @param timeoutMs Maximum time to wait in milliseconds.
     * @return true if navigation succeeded and reached the stage, false on timeout or failure.
     */
    suspend fun loadUrl(url: String, waitUntil: NavigationStage = NavigationStage.LOADED, timeoutMs: Long = 10_000L): Boolean

    /**
     * Loads a specific URL in the browser without suspending.
     * @param url The URL to load.
     */
    fun loadUrl(url: String)

    /**
     * Loads a specific URL and returns a CompletableFuture that completes when the requested stage is reached.
     */
    fun loadUrlAsync(url: String, waitUntil: NavigationStage = NavigationStage.LOADED, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>

    /**
     * Waits for the page navigation lifecycle to reach the specified stage.
     * @param stage The lifecycle stage to await.
     * @param timeoutMs Maximum time to wait in milliseconds.
     * @return true if the navigation reached the desired stage within timeoutMs, false otherwise.
     */
    suspend fun waitForNavigation(stage: NavigationStage = NavigationStage.LOADED, timeoutMs: Long = 10_000L): Boolean

    /**
     * Waits for the page navigation lifecycle to reach the specified stage and returns a CompletableFuture.
     */
    fun waitForNavigationAsync(stage: NavigationStage = NavigationStage.LOADED, timeoutMs: Long = 10_000L): CompletableFuture<Boolean>

    /**
     * Waits until there are no active in-flight network requests for a specific duration.
     * @param idleTimeMs The continuous duration (in ms) that network requests must be at zero.
     * @param maxTimeoutMs Maximum time to wait before giving up.
     * @param pollIntervalMs The frequency to check the network state (in ms).
     * @return true if network became idle, false on timeout.
     */
    suspend fun waitForNetworkIdle(idleTimeMs: Long = 500L, maxTimeoutMs: Long = 10_000L, pollIntervalMs: Long = 50L): Boolean

    /**
     * Waits until there are no active in-flight network requests and returns a CompletableFuture.
     */
    fun waitForNetworkIdleAsync(idleTimeMs: Long = 500L, maxTimeoutMs: Long = 10_000L, pollIntervalMs: Long = 50L): CompletableFuture<Boolean>

    /**
     * Loads raw HTML content directly into the browser.
     * @param html The raw HTML string.
     * @param url A dummy URL to use as the origin/base (default is "http://kromium.local/").
     */
    fun loadHtml(html: String, url: String = "http://kromium.local/")

    /** Navigates backwards in the history, if possible. */
    fun goBack()

    /** Navigates forwards in the history, if possible. */
    fun goForward()

    /** @return true if the browser can navigate backwards. */
    fun canGoBack(): Boolean

    /** @return true if the browser can navigate forwards. */
    fun canGoForward(): Boolean

    /** Reloads the current page. */
    fun reload(ignoreCache: Boolean = false)

    /** Stops any pending or current load operation. */
    fun stopLoad()

    /**
     * Observable state stream of the browser's navigation and loading status.
     */
    val navigationState: StateFlow<KromiumNavigationState>
}
