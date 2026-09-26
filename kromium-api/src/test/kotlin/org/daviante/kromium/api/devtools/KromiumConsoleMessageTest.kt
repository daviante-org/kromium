package org.daviante.kromium.api.devtools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KromiumConsoleMessageTest {

    @Test
    fun testConsoleMessageModelProperties() {
        val msg = KromiumConsoleMessage(
            level = KromiumConsoleMessageLevel.ERROR,
            message = "Uncaught TypeError: Cannot read property of undefined",
            source = "https://app.corp/bundle.js",
            line = 142
        )

        assertEquals(KromiumConsoleMessageLevel.ERROR, msg.level)
        assertEquals("Uncaught TypeError: Cannot read property of undefined", msg.message)
        assertEquals("https://app.corp/bundle.js", msg.source)
        assertEquals(142, msg.line)
    }

    @Test
    fun testConsoleMessageDefaults() {
        val msg = KromiumConsoleMessage(
            level = KromiumConsoleMessageLevel.INFO,
            message = "System ready"
        )

        assertEquals(KromiumConsoleMessageLevel.INFO, msg.level)
        assertEquals("System ready", msg.message)
        assertEquals("", msg.source)
        assertEquals(0, msg.line)
    }

    @Test
    fun testConsoleMessageListenerInvocation() {
        var received: KromiumConsoleMessage? = null
        val listener = KromiumConsoleMessageListener { message ->
            received = message
            true
        }

        val testMessage = KromiumConsoleMessage(
            level = KromiumConsoleMessageLevel.WARNING,
            message = "Feature deprecated",
            source = "index.html",
            line = 10
        )

        val handled = listener.onConsoleMessage(testMessage)
        assertTrue(handled)
        assertEquals(testMessage, received)
    }
}
