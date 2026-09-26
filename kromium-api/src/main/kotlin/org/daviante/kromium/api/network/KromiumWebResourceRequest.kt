package org.daviante.kromium.api.network

/**
 * Represents an outgoing web resource request that can be inspected and modified by a [KromiumRequestInterceptor].
 */
data class KromiumWebResourceRequest(
    val url: String,
    val method: String,
    val headers: MutableMap<String, String>,
    val isNavigation: Boolean,
    val isDownload: Boolean,
    val resourceType: KromiumResourceType = KromiumResourceType.UNKNOWN,
    val requestInitiator: String? = null
)
