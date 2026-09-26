package org.daviante.kromium.api.automation

/**
 * Facade for JavaScript execution and bridging.
 */
interface KromiumJsBridge {
    /**
     * Executes arbitrary JavaScript in the context of the current page.
     * @param code The JavaScript code to execute.
     */
    fun executeJavaScript(code: String)

    /**
     * Executes arbitrary JavaScript asynchronously and returns the stringified response.
     *
     * @param expression JavaScript expression to execute.
     * @param timeoutMs Maximum evaluation timeout in ms.
     * @param bindingTimeoutMs Maximum time in ms to poll for the query router function.
     * @param bindingIntervalMs Polling interval in ms between router binding checks.
     */
    suspend fun evaluateJavaScript(
        expression: String,
        timeoutMs: Long? = null,
        bindingTimeoutMs: Long? = null,
        bindingIntervalMs: Long? = null
    ): String?

    /**
     * Convenience function to fetch the complete HTML of the current document (`document.documentElement.outerHTML`).
     */
    suspend fun getHtml(): String

    /**
     * Asynchronously fetches the complete HTML of the current document returning a Java [CompletableFuture].
     */
    fun getHtmlAsync(): java.util.concurrent.CompletableFuture<String>

    /**
     * Convenience function to fetch the visible text content of the current document (`document.body.innerText`).
     */
    suspend fun getText(): String

    /**
     * Asynchronously fetches the visible text content of the current document returning a Java [CompletableFuture].
     */
    fun getTextAsync(): java.util.concurrent.CompletableFuture<String>

    /**
     * Attempts to resolve the URL of the page's favicon using DOM inspection.
     */
    suspend fun getFaviconUrl(): String?

    /**
     * Asynchronously resolves the favicon URL returning a Java [CompletableFuture].
     */
    fun getFaviconUrlAsync(): java.util.concurrent.CompletableFuture<String?>
}
