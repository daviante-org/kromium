package org.daviante.kromium.api.network


import java.net.URI

/**
 * Pure configuration for blocking or allowlisting network assets.
 * No hardcoded presets; developers must explicitly provide the extensions, URL patterns,
 * resource types, or custom predicates they want to filter.
 */
data class KromiumAssetFilter(
    val blockedExtensions: Set<String> = emptySet(),
    val blockedUrlPatterns: Set<String> = emptySet(),
    val blockedResourceTypes: Set<KromiumResourceType> = emptySet(),
    val blockFilter: ((request: KromiumWebResourceRequest) -> Boolean)? = null,

    val allowedExtensions: Set<String> = emptySet(),
    val allowedUrlPatterns: Set<String> = emptySet(),
    val allowedResourceTypes: Set<KromiumResourceType> = emptySet(),
    val allowFilter: ((request: KromiumWebResourceRequest) -> Boolean)? = null,
    
    val allowMainFrame: Boolean = true,
    val mode: AssetFilterMode = AssetFilterMode.BLOCKLIST
) {
    val isEnabled: Boolean
        get() = mode == AssetFilterMode.ALLOWLIST ||
                blockedExtensions.isNotEmpty() ||
                blockedUrlPatterns.isNotEmpty() ||
                blockedResourceTypes.isNotEmpty() ||
                blockFilter != null ||
                allowedExtensions.isNotEmpty() ||
                allowedUrlPatterns.isNotEmpty() ||
                allowedResourceTypes.isNotEmpty() ||
                allowFilter != null

    private fun extractCleanPath(url: String): String =
        try {
            val uri = URI(url)
            uri.path?.lowercase() ?: url.substringBefore('?').substringBefore('#').lowercase()
        } catch (_: Throwable) {
            url.substringBefore('?').substringBefore('#').lowercase()
        }

    private fun matchesAllowRules(
        request: KromiumWebResourceRequest,
        url: String,
        cleanPath: String,
        resourceType: KromiumResourceType
    ): Boolean {
        if (allowMainFrame && resourceType == KromiumResourceType.MAIN_FRAME) return true
        if (allowFilter?.invoke(request) == true) return true
        if (allowedResourceTypes.contains(resourceType)) return true
        if (allowedUrlPatterns.any { url.contains(it, ignoreCase = true) }) return true
        if (allowedExtensions.any { ext ->
            val normalized = if (ext.startsWith(".")) ext.lowercase() else ".$ext".lowercase()
            cleanPath.endsWith(normalized)
        }) return true
        return false
    }

    private fun matchesBlockRules(
        request: KromiumWebResourceRequest,
        url: String,
        cleanPath: String,
        resourceType: KromiumResourceType
    ): Boolean {
        if (blockFilter?.invoke(request) == true) return true
        if (blockedResourceTypes.contains(resourceType)) return true
        if (blockedUrlPatterns.any { url.contains(it, ignoreCase = true) }) return true
        if (blockedExtensions.any { ext ->
            val normalized = if (ext.startsWith(".")) ext.lowercase() else ".$ext".lowercase()
            cleanPath.endsWith(normalized)
        }) return true
        return false
    }

    /**
     * Determines whether a given request should be blocked based on configured filter rules and mode.
     */
    fun shouldBlock(request: KromiumWebResourceRequest): Boolean {
        if (!isEnabled) return false

        val url = request.url
        val cleanPath = extractCleanPath(url)
        val resourceType = request.resourceType

        return when (mode) {
            AssetFilterMode.ALLOWLIST -> {
                !matchesAllowRules(request, url, cleanPath, resourceType)
            }
            AssetFilterMode.BLOCKLIST -> {
                if (matchesAllowRules(request, url, cleanPath, resourceType)) {
                    false
                } else {
                    matchesBlockRules(request, url, cleanPath, resourceType)
                }
            }
            else -> false
        }
    }

    companion object {
        /**
         * Safely extracts the host component from a URL without throwing on non-standard URIs.
         */
        fun extractHost(url: String): String? {
            val trimmed = url.trim()
            try {
                val u = URI(trimmed)
                val h = u.host
                if (!h.isNullOrBlank()) return h.lowercase()
            } catch (_: Throwable) {}

            val afterScheme = when {
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
                trimmed.startsWith("ws://", ignoreCase = true) -> trimmed.substring(5)
                trimmed.startsWith("wss://", ignoreCase = true) -> trimmed.substring(6)
                else -> return null
            }
            val hostPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            val host = hostPort.substringBefore(':').trim().lowercase()
            return host.ifBlank { null }
        }

        /**
         * Helper to verify whether a URL's host matches the allowed host set (including subdomains).
         */
        fun isHostAllowed(url: String, allowedHosts: Set<String>): Boolean {
            if (allowedHosts.isEmpty()) return true
            val trimmed = url.trim()
            if (trimmed.startsWith("about:", ignoreCase = true) ||
                trimmed.startsWith("data:", ignoreCase = true) ||
                trimmed.startsWith("chrome:", ignoreCase = true) ||
                trimmed.startsWith("blob:", ignoreCase = true) ||
                trimmed.startsWith("file:", ignoreCase = true)) {
                return true
            }

            val lowerHost = extractHost(trimmed) ?: return false
            return allowedHosts.any { allowed ->
                val clean = allowed.trim().lowercase().removePrefix("*.")
                lowerHost == clean || lowerHost.endsWith(".$clean")
            }
        }
    }
}
