package org.daviante.kromium.jcef.core

import org.cef.CefClient
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.error.KromiumException
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JcefKromiumClientTest {

    @Test
    fun testCreateBrowserThrowsFrameworkRequiredWhenNoFrameworkSpecified() {
        val engine = mockk<JcefKromiumEngine>(relaxed = true)
        val cefClient = mockk<CefClient>(relaxed = true)
        val client = JcefKromiumClient(engine, cefClient, isHeadless = false, config = KromiumClientConfig.default())

        assertFailsWith<KromiumException.FrameworkRequired> {
            client.createBrowser("https://example.com")
        }
    }
}
