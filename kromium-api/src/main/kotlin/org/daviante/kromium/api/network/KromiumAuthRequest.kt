package org.daviante.kromium.api.network

/**
 * Details of an authentication challenge requested by a web server or proxy.
 */
data class KromiumAuthRequest(
    val isProxy: Boolean,
    val host: String,
    val port: Int,
    val realm: String,
    val scheme: String
)
