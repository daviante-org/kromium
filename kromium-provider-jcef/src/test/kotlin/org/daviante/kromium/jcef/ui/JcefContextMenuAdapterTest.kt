package org.daviante.kromium.jcef.ui

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumContextMenuListener
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import org.daviante.kromium.jcef.core.JcefKromiumClient
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JcefContextMenuAdapterTest {

    @Test
    fun testContextMenuSuppressionViaDisabledListener() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.enableContextMenus } returns true
        every { client.contextMenuListener } returns KromiumContextMenuListener.disabled()

        val adapter = JcefContextMenuAdapter(client, KromiumClientConfig.default())
        val model = mockk<CefMenuModel>(relaxed = true)

        adapter.onBeforeContextMenu(null, null, null, model)

        verify { model.clear() }
    }

    @Test
    fun testContextMenuDefaultPreservesModel() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.enableContextMenus } returns true
        every { client.contextMenuListener } returns KromiumContextMenuListener.defaultMenu()

        val adapter = JcefContextMenuAdapter(client, KromiumClientConfig.default())
        val model = mockk<CefMenuModel>(relaxed = true)

        adapter.onBeforeContextMenu(null, null, null, model)

        verify(exactly = 0) { model.clear() }
    }

    @Test
    fun testContextMenuListenerConstructsMenuAndExecutesCommand() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.enableContextMenus } returns true

        var actionExecuted = false
        val listener = KromiumContextMenuListener { builder, _ ->
            builder.clear()
            builder.item("Custom Item") {
                actionExecuted = true
            }
        }
        every { client.contextMenuListener } returns listener

        val adapter = JcefContextMenuAdapter(client, KromiumClientConfig.default())

        val params = mockk<CefContextMenuParams>(relaxed = true)
        every { params.xCoord } returns 150
        every { params.yCoord } returns 250
        val model = mockk<CefMenuModel>(relaxed = true)

        adapter.onBeforeContextMenu(null, null, params, model)

        verify { model.clear() }
        verify { model.addItem(KromiumMenuBuilder.USER_COMMAND_FIRST, "Custom Item") }

        val handled = adapter.onContextMenuCommand(null, null, params, KromiumMenuBuilder.USER_COMMAND_FIRST, 0)
        assertTrue(handled)
        assertTrue(actionExecuted)

        val unhandled = adapter.onContextMenuCommand(null, null, params, 99999, 0)
        assertFalse(unhandled)
    }

    @Test
    fun testContextMenuGloballyDisabled() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.enableContextMenus } returns false

        val adapter = JcefContextMenuAdapter(client, KromiumClientConfig.default())
        val model = mockk<CefMenuModel>(relaxed = true)

        adapter.onBeforeContextMenu(null, null, null, model)

        verify { model.clear() }
    }
}
