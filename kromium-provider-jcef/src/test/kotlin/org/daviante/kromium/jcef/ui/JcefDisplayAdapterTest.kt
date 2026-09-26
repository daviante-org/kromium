package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.devtools.KromiumConsoleMessage
import org.daviante.kromium.api.devtools.KromiumConsoleMessageLevel
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.jcef.core.JcefKromiumClient
import io.mockk.every
import io.mockk.mockk
import org.cef.CefSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JcefDisplayAdapterTest {

    @Test
    fun testConsoleMessageDelegation() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        var received: KromiumConsoleMessage? = null

        val listener = KromiumConsoleMessageListener { msg ->
            received = msg
            true
        }
        every { client.consoleMessageListener } returns listener

        val adapter = JcefDisplayAdapter(client, KromiumClientConfig.default())
        val handled = adapter.onConsoleMessage(
            null,
            CefSettings.LogSeverity.LOGSEVERITY_INFO,
            "Application initialized",
            "app.js",
            42
        )

        assertTrue(handled)
        assertNotNull(received)
        assertEquals(KromiumConsoleMessageLevel.INFO, received?.level)
        assertEquals("Application initialized", received?.message)
        assertEquals("app.js", received?.source)
        assertEquals(42, received?.line)
    }

    @Test
    fun testConsoleMessageSeverityMappings() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        var lastReceived: KromiumConsoleMessage? = null

        val listener = KromiumConsoleMessageListener { msg ->
            lastReceived = msg
            true
        }
        every { client.consoleMessageListener } returns listener

        val adapter = JcefDisplayAdapter(client, KromiumClientConfig.default())

        adapter.onConsoleMessage(null, CefSettings.LogSeverity.LOGSEVERITY_ERROR, "err", "", 1)
        assertEquals(KromiumConsoleMessageLevel.ERROR, lastReceived?.level)

        adapter.onConsoleMessage(null, CefSettings.LogSeverity.LOGSEVERITY_FATAL, "fatal", "", 2)
        assertEquals(KromiumConsoleMessageLevel.ERROR, lastReceived?.level)

        adapter.onConsoleMessage(null, CefSettings.LogSeverity.LOGSEVERITY_WARNING, "warn", "", 3)
        assertEquals(KromiumConsoleMessageLevel.WARNING, lastReceived?.level)

        adapter.onConsoleMessage(null, CefSettings.LogSeverity.LOGSEVERITY_VERBOSE, "verbose", "", 4)
        assertEquals(KromiumConsoleMessageLevel.DEBUG, lastReceived?.level)

        adapter.onConsoleMessage(null, CefSettings.LogSeverity.LOGSEVERITY_DEFAULT, "def", "", 5)
        assertEquals(KromiumConsoleMessageLevel.DEFAULT, lastReceived?.level)
    }
}
