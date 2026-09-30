package org.daviante.kromium.api.network

/**
 * Functional interface to intercept and modify HTTP/HTTPS requests or block unwanted domains.
 */
fun interface KromiumRequestInterceptor {
    /**
     * Intercepts an outgoing request.
     * Developers can mutate [request.headers] or return `true` to cancel/block the request.
     *
     * @param request The outgoing resource request.
     * @return `true` to block the request (e.g. ad/tracker blocking), or `false` to let it proceed.
     */
    fun intercept(request: KromiumWebResourceRequest): Boolean
}
