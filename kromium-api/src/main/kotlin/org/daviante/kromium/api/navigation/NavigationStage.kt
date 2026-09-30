package org.daviante.kromium.api.navigation

/**
 * Defines the navigation lifecycle stage to await during page navigation.
 */
enum class NavigationStage {
    /**
     * Main frame navigation has started.
     */
    STARTED,

    /**
     * Main frame document and static assets have finished loading.
     */
    LOADED,

    /**
     * Main frame has finished loading and active in-flight network requests have settled to zero
     * for at least a specific idle duration.
     */
    NETWORK_IDLE
}
