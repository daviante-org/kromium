package org.daviante.kromium.api.bootstrap

import org.daviante.kromium.core.platform.KromiumPlatformInfo

/**
 * Resolves the download URL for a specific platform dynamically.
 */
fun interface KromiumPlatformUrlResolver {
    fun resolveUrl(platform: KromiumPlatformInfo): String?
}
