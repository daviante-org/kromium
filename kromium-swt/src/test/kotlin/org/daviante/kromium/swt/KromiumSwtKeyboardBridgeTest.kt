package org.daviante.kromium.swt

import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.eclipse.swt.SWT
import kotlin.test.Test
import kotlin.test.assertEquals

class KromiumSwtKeyboardBridgeTest {

    @Test
    fun testKeyMappingTable() {
        assertEquals(KromiumKeyCodes.VK_ENTER, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.CR.code))
        assertEquals(KromiumKeyCodes.VK_ENTER, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.LF.code))
        assertEquals(KromiumKeyCodes.VK_BACK_SPACE, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.BS.code))
        assertEquals(KromiumKeyCodes.VK_TAB, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.TAB.code))
        assertEquals(KromiumKeyCodes.VK_ESCAPE, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ESC.code))
        assertEquals(KromiumKeyCodes.VK_DELETE, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.DEL.code))
        assertEquals(KromiumKeyCodes.VK_LEFT, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ARROW_LEFT))
        assertEquals(KromiumKeyCodes.VK_RIGHT, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ARROW_RIGHT))
        assertEquals(KromiumKeyCodes.VK_UP, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ARROW_UP))
        assertEquals(KromiumKeyCodes.VK_DOWN, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ARROW_DOWN))
        assertEquals(KromiumKeyCodes.VK_F5, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.F5))
        assertEquals(KromiumKeyCodes.VK_SHIFT, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.SHIFT))
        assertEquals(KromiumKeyCodes.VK_CONTROL, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.CTRL))
        assertEquals(KromiumKeyCodes.VK_ALT, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.ALT))
        assertEquals(KromiumKeyCodes.VK_META, KromiumSwtKeyboardBridge.mapSwtKeyCode(SWT.COMMAND))
    }

    @Test
    fun testAlphabeticalKeyMapping() {
        // Lowercase 'a' (97) should map to uppercase ASCII virtual key 'A' (65)
        assertEquals('A'.code, KromiumSwtKeyboardBridge.mapSwtKeyCode('a'.code))
        assertEquals('Z'.code, KromiumSwtKeyboardBridge.mapSwtKeyCode('z'.code))
        assertEquals('C'.code, KromiumSwtKeyboardBridge.mapSwtKeyCode('c'.code))
        assertEquals('V'.code, KromiumSwtKeyboardBridge.mapSwtKeyCode('v'.code))
    }

    @Test
    fun testPunctuationAndSymbolMapping() {
        assertEquals(KromiumKeyCodes.VK_QUOTE, KromiumSwtKeyboardBridge.mapSwtKeyCode('\''.code))
        assertEquals(KromiumKeyCodes.VK_BACK_QUOTE, KromiumSwtKeyboardBridge.mapSwtKeyCode('`'.code))
        assertEquals(KromiumKeyCodes.VK_SPACE, KromiumSwtKeyboardBridge.mapSwtKeyCode(' '.code))
    }

    @Test
    fun testFocusTrackingState() {
        KromiumSwtKeyboardBridge.setBrowserFocused(true)
        assertEquals(true, KromiumSwtKeyboardBridge.isBrowserActive)
        KromiumSwtKeyboardBridge.setBrowserFocused(false)
        assertEquals(false, KromiumSwtKeyboardBridge.isBrowserActive)
        KromiumSwtKeyboardBridge.setBrowserFocused(true)
    }

    @Test
    fun testDispatchPrintableKeyGeneratesSeparatePressedAndTyped() {
        val mockBrowser = io.mockk.mockk<org.daviante.kromium.api.core.KromiumBrowser>(relaxed = true)
        val capturedEvents = mutableListOf<org.daviante.kromium.api.ui.KromiumKeyEvent>()
        io.mockk.every { mockBrowser.view.sendKeyEvent(capture(capturedEvents)) } returns Unit

        val event = org.eclipse.swt.widgets.Event().apply {
            keyCode = 'd'.code
            character = 'd'
            stateMask = 0
        }

        // Dispatch key down
        KromiumSwtKeyboardBridge.dispatchSwtKeyEvent(mockBrowser, event, isKeyDown = true)

        assertEquals(2, capturedEvents.size)
        // PRESSED should carry VK_D and 'd' for macOS JCEF GetMacKeyCodeFromChar
        val pressed = capturedEvents[0]
        assertEquals(org.daviante.kromium.api.ui.KromiumKeyEventType.PRESSED, pressed.type)
        assertEquals('D'.code, pressed.keyCode)
        assertEquals('d', pressed.keyChar)

        // TYPED should carry VK_UNDEFINED and 'd'
        val typed = capturedEvents[1]
        assertEquals(org.daviante.kromium.api.ui.KromiumKeyEventType.TYPED, typed.type)
        assertEquals(org.daviante.kromium.api.ui.KromiumKeyCodes.VK_UNDEFINED, typed.keyCode)
        assertEquals('d', typed.keyChar)
    }

    @Test
    fun testDispatchActionKeyDoesNotGenerateTypedEvent() {
        val mockBrowser = io.mockk.mockk<org.daviante.kromium.api.core.KromiumBrowser>(relaxed = true)
        val capturedEvents = mutableListOf<org.daviante.kromium.api.ui.KromiumKeyEvent>()
        io.mockk.every { mockBrowser.view.sendKeyEvent(capture(capturedEvents)) } returns Unit

        val event = org.eclipse.swt.widgets.Event().apply {
            keyCode = SWT.CR.code
            character = '\r'
            stateMask = 0
        }

        KromiumSwtKeyboardBridge.dispatchSwtKeyEvent(mockBrowser, event, isKeyDown = true)

        // Only PRESSED should be generated for Enter, no TYPED
        assertEquals(1, capturedEvents.size)
        val pressed = capturedEvents[0]
        assertEquals(org.daviante.kromium.api.ui.KromiumKeyEventType.PRESSED, pressed.type)
        assertEquals(org.daviante.kromium.api.ui.KromiumKeyCodes.VK_ENTER, pressed.keyCode)
        assertEquals('\n', pressed.keyChar)
    }
}
