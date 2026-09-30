package org.daviante.kromium.core.network

import java.net.URI

object KromiumAssetFilter {

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

        // Fallback for malformed URLs
        val schemeIdx = trimmed.indexOf("://")
        if (schemeIdx == -1) return null
        val afterScheme = trimmed.substring(schemeIdx + 3)
        val hostPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = hostPort.substringBefore(':').trim().lowercase()
        return host.ifBlank { null }
    }

    /**
     * Helper to verify whether a URL's host matches the allowed host set (including subdomains).
     * Internal browser schemes ("about:", "data:", "chrome:", "blob:", "file:") are permitted.
     * Network requests ("http:", "https:", "ws:", "wss:") must match an allowed host entry.
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
