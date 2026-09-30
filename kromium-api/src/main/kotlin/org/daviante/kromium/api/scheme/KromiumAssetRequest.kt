package org.daviante.kromium.api.scheme

/**
 * Represents an incoming virtual HTTP request intercepted by a [KromiumAssetHandler].
 *
 * @property url The full URL string of the intercepted request.
 * @property method The HTTP method (e.g. "GET", "POST", "OPTIONS"). Defaults to "GET".
 * @property scheme The protocol scheme (e.g. "app", "https").
 * @property domain The host or authority domain component (e.g. "myapp", "localhost").
 * @property path The absolute request path without query strings (e.g. "/index.html", "/api/user").
 * @property queryString The raw URL query string (without the leading '?'), or null if absent.
 * @property queryParameters Parsed query parameters map.
 * @property headers Request headers map (case-insensitive keys).
 */
data class KromiumAssetRequest @JvmOverloads constructor(
    val url: String,
    val method: String = "GET",
    val scheme: String = "",
    val domain: String = "",
    val path: String = "/",
    val queryString: String? = null,
    val queryParameters: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap()
) {
    /**
     * Retrieves the value of a specific HTTP header in a case-insensitive manner.
     *
     * @param name The header name to lookup.
     * @return The header value, or null if absent.
     */
    fun getHeader(name: String): String? {
        val target = name.trim().lowercase()
        return headers.entries.firstOrNull { it.key.trim().lowercase() == target }?.value
    }

    /**
     * Retrieves a query parameter by key.
     *
     * @param key The query parameter name.
     * @return The query parameter value, or null if absent.
     */
    fun getQueryParam(key: String): String? = queryParameters[key]
}
