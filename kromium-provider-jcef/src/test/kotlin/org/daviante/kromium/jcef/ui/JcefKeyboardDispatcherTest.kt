package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumKeyEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcefKeyboardDispatcherTest {

    @Test
    fun testToAwtKeyEventMapping() {
        val mockBrowser = mockk<CefBrowser>(relaxed = true)

        // 1. Enter key
        val enterEvent = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_ENTER
        )
        val awtEnter = JcefKeyboardDispatcher.toAwtKeyEvent(mockBrowser, enterEvent)
        assertEquals(KeyEvent.KEY_PRESSED, awtEnter.id)
        assertEquals(KromiumKeyCodes.VK_ENTER, awtEnter.keyCode)
        assertEquals('\n', awtEnter.keyChar)

        // 2. Backspace key
        val backspaceEvent = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_BACK_SPACE
        )
        val awtBackspace = JcefKeyboardDispatcher.toAwtKeyEvent(mockBrowser, backspaceEvent)
        assertEquals(KeyEvent.KEY_PRESSED, awtBackspace.id)
        assertEquals(KromiumKeyCodes.VK_BACK_SPACE, awtBackspace.keyCode)
        assertEquals('\b', awtBackspace.keyChar)

        // 3. Tab key
        val tabEvent = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_TAB
        )
        val awtTab = JcefKeyboardDispatcher.toAwtKeyEvent(mockBrowser, tabEvent)
        assertEquals(KromiumKeyCodes.VK_TAB, awtTab.keyCode)
        assertEquals('\t', awtTab.keyChar)

        // 4. Modifiers
        val ctrlShiftA = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_A,
            modifiers = KromiumKeyEvent.CTRL_MASK or KromiumKeyEvent.SHIFT_MASK
        )
        val awtCtrlShiftA = JcefKeyboardDispatcher.toAwtKeyEvent(mockBrowser, ctrlShiftA)
        assertTrue(awtCtrlShiftA.modifiersEx and InputEvent.CTRL_DOWN_MASK != 0)
        assertTrue(awtCtrlShiftA.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0)
        assertEquals(KromiumKeyCodes.VK_A, awtCtrlShiftA.keyCode)

        // 5. Key typed event
        val typedEvent = KromiumKeyEvent(
            type = KromiumKeyEventType.TYPED,
            keyChar = 'z'
        )
        val awtTyped = JcefKeyboardDispatcher.toAwtKeyEvent(mockBrowser, typedEvent)
        assertEquals(KeyEvent.KEY_TYPED, awtTyped.id)
        assertEquals(KeyEvent.VK_UNDEFINED, awtTyped.keyCode)
        assertEquals('z', awtTyped.keyChar)

        // 6. Verify native rawCode and scancode injected into AWT KeyEvent
        assertTrue(awtEnter.paramString().contains("rawCode=13"), "Enter must have rawCode=13 (Windows VK_RETURN)")
        assertTrue(awtBackspace.paramString().contains("rawCode=8"), "Backspace must have rawCode=8 (Windows VK_BACK)")
        assertEquals(28L, JcefKeyboardDispatcher.mapToScanCode(KromiumKeyCodes.VK_ENTER))
        assertEquals(14L, JcefKeyboardDispatcher.mapToScanCode(KromiumKeyCodes.VK_BACK_SPACE))
        assertEquals(13L, JcefKeyboardDispatcher.mapToWindowsRawCode(enterEvent, KromiumKeyCodes.VK_ENTER, '\n'))
    }

    @Test
    fun testDispatchForwardsToBrowser() {
        val mockBrowser = mockk<CefBrowser>(relaxed = true)
        val slot = slot<KeyEvent>()
        every { mockBrowser.sendKeyEvent(capture(slot)) } returns Unit

        val event = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_LEFT
        )
        val handled = JcefKeyboardDispatcher.dispatch(mockBrowser, event)

        assertTrue(handled)
        verify(exactly = 1) { mockBrowser.setFocus(true) }
        verify(exactly = 1) { mockBrowser.sendKeyEvent(any()) }
        assertEquals(KromiumKeyCodes.VK_LEFT, slot.captured.keyCode)
    }

    @Test
    fun testDispatchInterceptsBrowserShortcut() {
        val mockBrowser = mockk<CefBrowser>(relaxed = true)
        val mockFrame = mockk<CefFrame>(relaxed = true)
        every { mockBrowser.focusedFrame } returns mockFrame

        val isMac = System.getProperty("os.name", "").lowercase().contains("mac")
        val copyModifier = if (isMac) KromiumKeyEvent.META_MASK else KromiumKeyEvent.CTRL_MASK

        val copyEvent = KromiumKeyEvent(
            type = KromiumKeyEventType.PRESSED,
            keyCode = KromiumKeyCodes.VK_C,
            modifiers = copyModifier
        )

        val handled = JcefKeyboardDispatcher.dispatch(mockBrowser, copyEvent)
        assertTrue(handled)
        verify(exactly = 1) { mockFrame.copy() }
        verify(exactly = 0) { mockBrowser.sendKeyEvent(any()) }
    }
}
