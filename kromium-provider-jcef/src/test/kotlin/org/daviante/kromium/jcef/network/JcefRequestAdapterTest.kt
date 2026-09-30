package org.daviante.kromium.jcef.network

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.KromiumAuthResponse
import org.daviante.kromium.api.network.KromiumRequestInterceptor
import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.jcef.core.JcefKromiumClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefAuthCallback
import org.cef.network.CefRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JcefRequestAdapterTest {

    @Test
    fun testUserAgentAndDntInjection() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.doNotTrack } returns true
        every { client.requestInterceptor } returns null
        every { client.browsers } returns mutableListOf()

        val config = KromiumClientConfig(userAgent = "Kromium/1.0", doNotTrack = true)
        val adapter = JcefRequestAdapter(client, config)

        val request = mockk<CefRequest>(relaxed = true)
        every { request.url } returns "https://example.com"
        every { request.method } returns "GET"

        val resourceHandler = adapter.getResourceRequestHandler(null, null, request, false, false, null, null)
        assertNotNull(resourceHandler)

        val blocked = resourceHandler.onBeforeResourceLoad(null, null, request)
        assertFalse(blocked)

        verify { request.setHeaderByName("User-Agent", "Kromium/1.0", true) }
        verify { request.setHeaderByName("DNT", "1", true) }
        verify { request.setHeaderByName("Sec-GPC", "1", true) }
    }

    @Test
    fun testRequestInterceptorBlock() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.doNotTrack } returns false
        every { client.browsers } returns mutableListOf()
        val interceptor = KromiumRequestInterceptor { req -> req.url.contains("tracker") }
        every { client.requestInterceptor } returns interceptor

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val request = mockk<CefRequest>(relaxed = true)
        every { request.url } returns "https://tracker.com/beacon"

        val resourceHandler = adapter.getResourceRequestHandler(null, null, request, false, false, null, null)
        assertNotNull(resourceHandler)

        val blocked = resourceHandler.onBeforeResourceLoad(null, null, request)
        assertTrue(blocked, "Interceptor should have blocked the tracking request")
    }

    @Test
    fun testRequestInterceptorMutateHeaders() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.doNotTrack } returns false
        every { client.browsers } returns mutableListOf()
        val interceptor = KromiumRequestInterceptor { req ->
            req.headers["X-Custom-Auth"] = "secret-token"
            false
        }
        every { client.requestInterceptor } returns interceptor

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val request = mockk<CefRequest>(relaxed = true)
        every { request.url } returns "https://api.example.com/data"
        every { request.getHeaderMap(any()) } answers {
            val map = firstArg<Map<String, String>>() as? MutableMap<String, String>
            map?.put("Existing", "Value")
        }

        val resourceHandler = adapter.getResourceRequestHandler(null, null, request, false, false, null, null)
        assertNotNull(resourceHandler)

        val blocked = resourceHandler.onBeforeResourceLoad(null, null, request)
        assertFalse(blocked)

        verify { request.setHeaderByName("X-Custom-Auth", "secret-token", true) }
    }

    @Test
    fun testAuthListenerProceed() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.authListener } returns KromiumAuthListener {
            KromiumAuthResponse.proceed("alice", "pass123")
        }

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val callback = mockk<CefAuthCallback>(relaxed = true)

        val handled = adapter.getAuthCredentials(
            null, "https://secure.com", false, "secure.com", 443, "realm", "basic", callback
        )
        assertTrue(handled)
        verify { callback.Continue("alice", "pass123") }
    }

    @Test
    fun testAuthListenerCancel() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.authListener } returns KromiumAuthListener {
            KromiumAuthResponse.cancel()
        }

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val callback = mockk<CefAuthCallback>(relaxed = true)

        val handled = adapter.getAuthCredentials(
            null, "https://secure.com", false, "secure.com", 443, "realm", "basic", callback
        )
        assertFalse(handled)
        verify { callback.cancel() }
    }

    @Test
    fun testProxyAuthFallback() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.authListener } returns null
        every { client.currentProxy } returns KromiumProxy.http(
            host = "proxy.corp.internal",
            port = 8080,
            username = "proxyuser",
            password = "proxypassword"
        )

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val callback = mockk<CefAuthCallback>(relaxed = true)

        val handled = adapter.getAuthCredentials(
            null, null, true, "proxy.corp.internal", 8080, null, null, callback
        )
        assertTrue(handled)
        verify { callback.Continue("proxyuser", "proxypassword") }
    }

    @Test
    fun testMultiProtocolProxyAuthFallback() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.authListener } returns null
        every { client.currentProxy } returns KromiumProxy.multiProtocol(
            http = "http://httpuser:httppass@http-proxy.corp:8080",
            https = "https://httpsuser:httpspass@https-proxy.corp:8443"
        )

        val adapter = JcefRequestAdapter(client, KromiumClientConfig.default())
        val callback = mockk<CefAuthCallback>(relaxed = true)

        val handled = adapter.getAuthCredentials(
            null, null, true, "http-proxy.corp", 8080, null, null, callback
        )
        assertTrue(handled)
        verify { callback.Continue("httpuser", "httppass") }
    }
}
