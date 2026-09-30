package org.daviante.kromium.jcef.scheme

import org.daviante.kromium.api.scheme.KromiumAssetHandler
import org.daviante.kromium.api.scheme.KromiumAssetResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefCallback
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcefResourceHandlerAdapterTest {

    @Test
    fun testJcefAssetRequestMapper() {
        val cefRequest = mockk<CefRequest>()
        every { cefRequest.url } returns "app://myapp/dashboard?tab=profile"
        every { cefRequest.method } returns "GET"
        every { cefRequest.getHeaderMap(any()) } answers {
            val map = firstArg<Map<String, String>>() as? MutableMap<String, String>
            map?.put("X-Custom", "TestHeader")
        }

        val mapped = JcefAssetRequestMapper.map(cefRequest)
        assertEquals("app://myapp/dashboard?tab=profile", mapped.url)
        assertEquals("GET", mapped.method)
        assertEquals("app", mapped.scheme)
        assertEquals("myapp", mapped.domain)
        assertEquals("/dashboard", mapped.path)
        assertEquals("tab=profile", mapped.queryString)
        assertEquals("profile", mapped.getQueryParam("tab"))
        assertEquals("TestHeader", mapped.getHeader("x-custom"))
    }

    @Test
    fun testJcefResourceHandlerAdapterProcessRequestAndHeaders() {
        val handler = KromiumAssetHandler { req ->
            if (req.path == "/data.json") {
                KromiumAssetResponse.json("""{"hello":"world"}""").withHeader("X-Server", "Kromium")
            } else {
                null
            }
        }

        val adapter = JcefResourceHandlerAdapter(handler)

        // 1. Successful request
        val okRequest = mockk<CefRequest>()
        every { okRequest.url } returns "app://myapp/data.json"
        every { okRequest.method } returns "GET"
        every { okRequest.getHeaderMap(any()) } returns Unit

        val callback = mockk<CefCallback>(relaxed = true)
        val processed = adapter.processRequest(okRequest, callback)
        assertTrue(processed)
        verify(exactly = 1) { callback.Continue() }

        // 2. Get headers
        val response = mockk<CefResponse>(relaxed = true)
        val lengthRef = IntRef()
        val redirectUrl = StringRef()

        adapter.getResponseHeaders(response, lengthRef, redirectUrl)
        verify { response.status = 200 }
        verify { response.mimeType = "application/json; charset=utf-8" }
        assertEquals(17, lengthRef.get()) // length of '{"hello":"world"}'

        // 3. Read stream
        val buffer = ByteArray(64)
        val bytesRead = IntRef()
        val hasMore = adapter.readResponse(buffer, buffer.size, bytesRead, callback)
        assertTrue(hasMore)
        assertEquals(17, bytesRead.get())
        assertEquals("""{"hello":"world"}""", String(buffer, 0, bytesRead.get()))
    }
}
