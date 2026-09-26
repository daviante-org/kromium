package org.daviante.kromium.jcef.ui

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefMenuModel
import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.ui.KromiumContextMenuParams
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcefMenuBuilderTest {

    @Test
    fun testBuilderRegistersItemsAndGeneratesCommandIds() {
        val mockModel = mockk<CefMenuModel>(relaxed = true)
        val mockSubModel = mockk<CefMenuModel>(relaxed = true)
        every { mockModel.addSubMenu(any(), any()) } returns mockSubModel
        every { mockModel.count } returns 4

        val actionMap = mutableMapOf<Int, (KromiumContextMenuContext) -> Unit>()
        val params = KromiumContextMenuParams(x = 10, y = 20)
        val context = KromiumContextMenuContext(browser = null, params = params)

        val builder = JcefMenuBuilder(mockModel, context, actionMap)

        builder.clear()
        verify(exactly = 1) { mockModel.clear() }

        var itemClicked = false
        builder.item("Action 1") { itemClicked = true }
        verify { mockModel.addItem(KromiumMenuBuilder.USER_COMMAND_FIRST, "Action 1") }
        verify { mockModel.setEnabled(KromiumMenuBuilder.USER_COMMAND_FIRST, true) }

        var checkToggled = false
        val checkCmdId = KromiumMenuBuilder.USER_COMMAND_FIRST + 1
        builder.checkItem("Check 1", checked = false) { checkToggled = it }
        verify { mockModel.addCheckItem(checkCmdId, "Check 1") }
        verify { mockModel.setChecked(checkCmdId, false) }

        var radioSelected = false
        val radioCmdId = KromiumMenuBuilder.USER_COMMAND_FIRST + 2
        builder.radioItem("Radio 1", checked = true, groupId = 100) { radioSelected = true }
        verify { mockModel.addRadioItem(radioCmdId, "Radio 1", 100) }
        verify { mockModel.setChecked(radioCmdId, true) }

        builder.separator()
        verify { mockModel.addSeparator() }

        var subItemClicked = false
        val subMenuCmdId = KromiumMenuBuilder.USER_COMMAND_FIRST + 3
        val subItemCmdId = KromiumMenuBuilder.USER_COMMAND_FIRST + 4
        builder.subMenu("Submenu 1") {
            item("Sub Action") { subItemClicked = true }
        }
        verify { mockModel.addSubMenu(subMenuCmdId, "Submenu 1") }
        verify { mockSubModel.addItem(subItemCmdId, "Sub Action") }

        assertEquals(4, builder.count)

        // Verify actions dispatched through actionMap
        actionMap[KromiumMenuBuilder.USER_COMMAND_FIRST]?.invoke(context)
        assertTrue(itemClicked)

        actionMap[checkCmdId]?.invoke(context)
        assertTrue(checkToggled) // toggled to true

        actionMap[radioCmdId]?.invoke(context)
        assertTrue(radioSelected)

        actionMap[subItemCmdId]?.invoke(context)
        assertTrue(subItemClicked)
    }

    @Test
    fun testNativeCommandShortcuts() {
        val mockModel = mockk<CefMenuModel>(relaxed = true)
        val actionMap = mutableMapOf<Int, (KromiumContextMenuContext) -> Unit>()
        val params = KromiumContextMenuParams(x = 0, y = 0)
        val context = KromiumContextMenuContext(browser = null, params = params)

        val builder = JcefMenuBuilder(mockModel, context, actionMap)

        builder.copy()
        builder.cut()
        builder.paste()
        builder.selectAll()
        builder.back()
        builder.forward()
        builder.reload()
        builder.print()
        builder.viewSource()

        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_COPY, "Copy") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_CUT, "Cut") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_PASTE, "Paste") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_SELECT_ALL, "Select All") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_BACK, "Back") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_FORWARD, "Forward") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_RELOAD, "Reload") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_PRINT, "Print...") }
        verify { mockModel.addItem(CefMenuModel.MenuId.MENU_ID_VIEW_SOURCE, "View Page Source") }
    }
}
