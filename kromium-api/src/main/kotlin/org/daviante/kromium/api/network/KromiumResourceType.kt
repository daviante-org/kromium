package org.daviante.kromium.api.network

/**
 * Platform-agnostic representation of web resource types for network interception and asset filtering.
 */
enum class KromiumResourceType {
    MAIN_FRAME,
    SUB_FRAME,
    STYLESHEET,
    SCRIPT,
    IMAGE,
    FONT_RESOURCE,
    SUB_RESOURCE,
    OBJECT,
    MEDIA,
    WORKER,
    SHARED_WORKER,
    PREFETCH,
    FAVICON,
    XHR,
    PING,
    SERVICE_WORKER,
    CSP_REPORT,
    PLUGIN_RESOURCE,
    NAVIGATION_PRELOAD_MAIN_FRAME,
    NAVIGATION_PRELOAD_SUB_FRAME,
    UNKNOWN
}
