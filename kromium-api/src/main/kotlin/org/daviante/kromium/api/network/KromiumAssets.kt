package org.daviante.kromium.api.network

/**
 * Facade for managing network asset blocking (e.g. images, media, fonts).
 */
interface KromiumAssets {
    /**
     * The active asset filter configuration, or null if filtering is disabled.
     */
    var filter: KromiumAssetFilter?
}
