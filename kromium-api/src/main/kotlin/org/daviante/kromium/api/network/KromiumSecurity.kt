package org.daviante.kromium.api.network

/**
 * Facade for managing browser security rules such as host locking.
 */
interface KromiumSecurity {
    /**
     * Whitelist of allowed hostnames/domains. Navigations outside these domains will be rejected.
     */
    var hostLock: Set<String>?

    /**
     * When true, host lock also restricts subresource network requests (scripts, xhr, fetch).
     */
    var hostLockSubresources: Boolean

    /**
     * When true, host lock also restricts navigations in subframes (iframes).
     * Defaults to false, allowing embedded verification widgets to navigate within iframes
     * without violating host locking.
     */
    var hostLockSubframes: Boolean
}
