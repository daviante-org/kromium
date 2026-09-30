package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumPopupListener
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JcefLifeSpanAdapterTest {

    @Test
    fun testPopupBlockedByListener() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.popupListener } returns KromiumPopupListener { url -> url.contains("ads") }

        val adapter = JcefLifeSpanAdapter(client, KromiumClientConfig.default())

        val blocked = adapter.onBeforePopup(null, null, "https://ads.example.com/popup", null)
        assertTrue(blocked, "Popup matching filter should be blocked")

        val allowed = adapter.onBeforePopup(null, null, "https://example.com/window", null)
        assertFalse(allowed, "Popup not matching filter should be allowed")
    }

    @Test
    fun testPopupAllowedByDefaultWhenNoListener() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.popupListener } returns null

        val adapter = JcefLifeSpanAdapter(client, KromiumClientConfig.default())
        val result = adapter.onBeforePopup(null, null, "https://example.com/popup", null)
        assertFalse(result, "Popup should be allowed if no popupListener is registered")
    }
}
