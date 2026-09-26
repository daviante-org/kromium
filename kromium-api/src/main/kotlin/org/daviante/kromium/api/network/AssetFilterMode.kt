package org.daviante.kromium.api.network

/**
 * Operating mode for [KromiumAssetFilter].
 */
enum class AssetFilterMode {
    /**
     * Default allow: requests proceed unless matched by a blocking rule.
     * Any configured allow rules act as bypass exceptions that unblock specific requests.
     */
    BLOCKLIST,

    /**
     * Default block: all requests are blocked unless explicitly permitted by an allow rule.
     */
    ALLOWLIST
}