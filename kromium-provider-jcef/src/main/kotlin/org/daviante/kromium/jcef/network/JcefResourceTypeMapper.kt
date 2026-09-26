package org.daviante.kromium.jcef.network

import org.cef.network.CefRequest
import org.daviante.kromium.api.network.KromiumResourceType

object JcefResourceTypeMapper {
    fun map(type: CefRequest.ResourceType?): KromiumResourceType {
        if (type == null) return KromiumResourceType.UNKNOWN
        return when (type) {
            CefRequest.ResourceType.RT_MAIN_FRAME -> KromiumResourceType.MAIN_FRAME
            CefRequest.ResourceType.RT_SUB_FRAME -> KromiumResourceType.SUB_FRAME
            CefRequest.ResourceType.RT_STYLESHEET -> KromiumResourceType.STYLESHEET
            CefRequest.ResourceType.RT_SCRIPT -> KromiumResourceType.SCRIPT
            CefRequest.ResourceType.RT_IMAGE -> KromiumResourceType.IMAGE
            CefRequest.ResourceType.RT_FONT_RESOURCE -> KromiumResourceType.FONT_RESOURCE
            CefRequest.ResourceType.RT_SUB_RESOURCE -> KromiumResourceType.SUB_RESOURCE
            CefRequest.ResourceType.RT_OBJECT -> KromiumResourceType.OBJECT
            CefRequest.ResourceType.RT_MEDIA -> KromiumResourceType.MEDIA
            CefRequest.ResourceType.RT_WORKER -> KromiumResourceType.WORKER
            CefRequest.ResourceType.RT_SHARED_WORKER -> KromiumResourceType.SHARED_WORKER
            CefRequest.ResourceType.RT_PREFETCH -> KromiumResourceType.PREFETCH
            CefRequest.ResourceType.RT_FAVICON -> KromiumResourceType.FAVICON
            CefRequest.ResourceType.RT_XHR -> KromiumResourceType.XHR
            CefRequest.ResourceType.RT_PING -> KromiumResourceType.PING
            CefRequest.ResourceType.RT_SERVICE_WORKER -> KromiumResourceType.SERVICE_WORKER
            CefRequest.ResourceType.RT_CSP_REPORT -> KromiumResourceType.CSP_REPORT
            CefRequest.ResourceType.RT_PLUGIN_RESOURCE -> KromiumResourceType.PLUGIN_RESOURCE
            CefRequest.ResourceType.RT_NAVIGATION_PRELOAD_MAIN_FRAME -> KromiumResourceType.NAVIGATION_PRELOAD_MAIN_FRAME
            CefRequest.ResourceType.RT_NAVIGATION_PRELOAD_SUB_FRAME -> KromiumResourceType.NAVIGATION_PRELOAD_SUB_FRAME
            else -> KromiumResourceType.UNKNOWN
        }
    }
}
