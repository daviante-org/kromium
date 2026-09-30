package org.daviante.kromium.jcef.scheme

import org.daviante.kromium.api.scheme.KromiumAssetRequest
import org.cef.network.CefRequest
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.HashMap

/**
 * Maps native JCEF [CefRequest] instances into provider-agnostic [KromiumAssetRequest] objects.
 */
internal object JcefAssetRequestMapper {

    fun map(cefRequest: CefRequest): KromiumAssetRequest {
        val urlString = cefRequest.url ?: ""
        val method = cefRequest.method ?: "GET"

        val headerMap = HashMap<String, String>()
        try {
            cefRequest.getHeaderMap(headerMap)
        } catch (_: Throwable) {}

        var scheme = ""
        var domain = ""
        var path = "/"
        var queryString: String? = null
        val queryParams = HashMap<String, String>()

        try {
            val uri = URI.create(urlString)
            scheme = uri.scheme ?: ""
            domain = uri.host ?: uri.authority ?: ""
            val rawPath = uri.path
            path = if (!rawPath.isNullOrBlank()) rawPath else "/"
            queryString = uri.rawQuery

            if (!queryString.isNullOrBlank()) {
                val pairs = queryString.split("&")
                for (pair in pairs) {
                    val idx = pair.indexOf('=')
                    if (idx > 0) {
                        val key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8)
                        val value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                        queryParams[key] = value
                    } else if (pair.isNotEmpty()) {
                        val key = URLDecoder.decode(pair, StandardCharsets.UTF_8)
                        queryParams[key] = ""
                    }
                }
            }
        } catch (_: Throwable) {
            val colonIdx = urlString.indexOf("://")
            if (colonIdx > 0) {
                scheme = urlString.substring(0, colonIdx)
                val rest = urlString.substring(colonIdx + 3)
                val slashIdx = rest.indexOf('/')
                val questionIdx = rest.indexOf('?')
                if (slashIdx > 0) {
                    domain = rest.substring(0, slashIdx)
                    val endPath = if (questionIdx > slashIdx) questionIdx else rest.length
                    path = rest.substring(slashIdx, endPath)
                    if (questionIdx > 0 && questionIdx < rest.length - 1) {
                        queryString = rest.substring(questionIdx + 1)
                    }
                } else if (questionIdx > 0) {
                    domain = rest.substring(0, questionIdx)
                    path = "/"
                    queryString = rest.substring(questionIdx + 1)
                } else {
                    domain = rest
                    path = "/"
                }
            }
        }

        return KromiumAssetRequest(
            url = urlString,
            method = method,
            scheme = scheme,
            domain = domain,
            path = path,
            queryString = queryString,
            queryParameters = Collections.unmodifiableMap(queryParams),
            headers = Collections.unmodifiableMap(headerMap)
        )
    }
}
