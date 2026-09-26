package org.daviante.kromium.jcef.ui

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefJSDialogCallback
import org.cef.handler.CefJSDialogHandler
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumJsDialogType
import org.daviante.kromium.jcef.core.JcefKromiumClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JcefJSDialogAdapterTest {

    @Test
    fun testOnJSDialogWithoutListenerReturnsFalse() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.jsDialogListener } returns null
        val config = KromiumClientConfig.default()

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
            "Hello World",
            "",
            callback,
            null
        )

        assertFalse(handled)
        verify(exactly = 0) { callback.Continue(any(), any()) }
    }

    @Test
    fun testOnJSDialogAlertHandling() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        val config = KromiumClientConfig.default()

        var listenerInvoked = false
        val listener = KromiumJsDialogListener { dialog ->
            listenerInvoked = true
            assertEquals(KromiumJsDialogType.ALERT, dialog.type)
            assertEquals("Alert message", dialog.message)
            assertEquals("https://example.com", dialog.originUrl)
            dialog.confirm()
            true
        }
        every { client.jsDialogListener } returns listener

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
            "Alert message",
            "",
            callback,
            null
        )

        assertTrue(handled)
        assertTrue(listenerInvoked)
        verify(exactly = 1) { callback.Continue(true, "") }
    }

    @Test
    fun testOnJSDialogPromptHandling() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        val config = KromiumClientConfig.default()

        val listener = KromiumJsDialogListener { dialog ->
            assertEquals(KromiumJsDialogType.PROMPT, dialog.type)
            assertEquals("DefaultUser", dialog.defaultPromptText)
            dialog.confirm("CustomUser")
            true
        }
        every { client.jsDialogListener } returns listener

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT,
            "Please enter your username",
            "DefaultUser",
            callback,
            null
        )

        assertTrue(handled)
        verify(exactly = 1) { callback.Continue(true, "CustomUser") }
    }

    @Test
    fun testOnJSDialogCancelHandling() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        val config = KromiumClientConfig.default()

        val listener = KromiumJsDialogListener { dialog ->
            assertEquals(KromiumJsDialogType.CONFIRM, dialog.type)
            dialog.cancel()
            true
        }
        every { client.jsDialogListener } returns listener

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM,
            "Are you sure?",
            "",
            callback,
            null
        )

        assertTrue(handled)
        verify(exactly = 1) { callback.Continue(false, "") }
    }

    @Test
    fun testListenerReturningFalseReturnsFalse() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        val config = KromiumClientConfig.default()

        val listener = KromiumJsDialogListener {
            false
        }
        every { client.jsDialogListener } returns listener

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
            "Unmanaged alert",
            "",
            callback,
            null
        )

        assertFalse(handled)
        verify(exactly = 0) { callback.Continue(any(), any()) }
    }

    @Test
    fun testListenerExceptionDoesNotThrowAndReturnsFalse() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        val config = KromiumClientConfig.default()

        val listener = KromiumJsDialogListener {
            throw RuntimeException("UI framework exception")
        }
        every { client.jsDialogListener } returns listener

        val adapter = JcefJSDialogAdapter(client, config)
        val callback = mockk<CefJSDialogCallback>(relaxed = true)

        val handled = adapter.onJSDialog(
            null,
            "https://example.com",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
            "Boom",
            "",
            callback,
            null
        )

        assertFalse(handled)
    }
}
