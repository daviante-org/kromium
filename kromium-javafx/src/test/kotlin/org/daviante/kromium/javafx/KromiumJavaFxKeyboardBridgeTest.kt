package org.daviante.kromium.javafx

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumKeyEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KromiumJavaFxKeyboardBridgeTest {

    @Test
    fun testActionKeyMappings() {
        // 1. Enter key pressed
        val enterPressed = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "\r",
            KeyCode.ENTER,
            false,
            false,
            false,
            false
        )
        val kromiumEnter = KromiumJavaFxKeyboardBridge.toKromiumEvent(enterPressed)
        assertNotNull(kromiumEnter)
        assertEquals(KromiumKeyEventType.PRESSED, kromiumEnter.type)
        assertEquals(KromiumKeyCodes.VK_ENTER, kromiumEnter.keyCode)
        assertEquals('\n', kromiumEnter.keyChar)

        // 2. Backspace key pressed
        val backspacePressed = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.BACK_SPACE,
            false,
            false,
            false,
            false
        )
        val kromiumBackspace = KromiumJavaFxKeyboardBridge.toKromiumEvent(backspacePressed)
        assertNotNull(kromiumBackspace)
        assertEquals(KromiumKeyEventType.PRESSED, kromiumBackspace.type)
        assertEquals(KromiumKeyCodes.VK_BACK_SPACE, kromiumBackspace.keyCode)
        assertEquals('\b', kromiumBackspace.keyChar)

        // 3. Tab key pressed
        val tabPressed = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "\t",
            KeyCode.TAB,
            false,
            false,
            false,
            false
        )
        val kromiumTab = KromiumJavaFxKeyboardBridge.toKromiumEvent(tabPressed)
        assertNotNull(kromiumTab)
        assertEquals(KromiumKeyCodes.VK_TAB, kromiumTab.keyCode)
        assertEquals('\t', kromiumTab.keyChar)

        // 4. Delete key pressed
        val deletePressed = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.DELETE,
            false,
            false,
            false,
            false
        )
        val kromiumDelete = KromiumJavaFxKeyboardBridge.toKromiumEvent(deletePressed)
        assertNotNull(kromiumDelete)
        assertEquals(KromiumKeyCodes.VK_DELETE, kromiumDelete.keyCode)

        // 5. Arrow keys
        val leftPressed = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.LEFT,
            false,
            false,
            false,
            false
        )
        val kromiumLeft = KromiumJavaFxKeyboardBridge.toKromiumEvent(leftPressed)
        assertNotNull(kromiumLeft)
        assertEquals(KromiumKeyCodes.VK_LEFT, kromiumLeft.keyCode)
    }

    @Test
    fun testKeyTypedNormalization() {
        // Enter typed with carriage return
        val enterTyped = KeyEvent(
            KeyEvent.KEY_TYPED,
            "\r",
            "",
            KeyCode.UNDEFINED,
            false,
            false,
            false,
            false
        )
        val kromiumEnterTyped = KromiumJavaFxKeyboardBridge.toKromiumEvent(enterTyped)
        assertNotNull(kromiumEnterTyped)
        assertEquals(KromiumKeyEventType.TYPED, kromiumEnterTyped.type)
        assertEquals('\n', kromiumEnterTyped.keyChar)

        // Letter typed
        val letterTyped = KeyEvent(
            KeyEvent.KEY_TYPED,
            "a",
            "",
            KeyCode.UNDEFINED,
            false,
            false,
            false,
            false
        )
        val kromiumLetter = KromiumJavaFxKeyboardBridge.toKromiumEvent(letterTyped)
        assertNotNull(kromiumLetter)
        assertEquals(KromiumKeyEventType.TYPED, kromiumLetter.type)
        assertEquals('a', kromiumLetter.keyChar)
    }

    @Test
    fun testCrossPlatformModifiers() {
        // Ctrl + Shift + C (Windows)
        val ctrlShiftC = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "c",
            KeyCode.C,
            true,  // shift
            true,  // control
            false, // alt
            false  // meta
        )
        val kromiumCtrlShiftC = KromiumJavaFxKeyboardBridge.toKromiumEvent(ctrlShiftC)
        assertNotNull(kromiumCtrlShiftC)
        assertTrue(kromiumCtrlShiftC.isControlDown)
        assertTrue(kromiumCtrlShiftC.isShiftDown)
        assertEquals(KromiumKeyCodes.VK_C, kromiumCtrlShiftC.keyCode)

        // Command + V (macOS)
        val cmdV = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "v",
            KeyCode.V,
            false, // shift
            false, // control
            false, // alt
            true   // meta (Command on Mac)
        )
        val kromiumCmdV = KromiumJavaFxKeyboardBridge.toKromiumEvent(cmdV)
        assertNotNull(kromiumCmdV)
        assertTrue(kromiumCmdV.isMetaDown)
        assertEquals(KromiumKeyCodes.VK_V, kromiumCmdV.keyCode)
    }
    @Test
    fun testInstallOnSceneWithFocusCheck() {
        org.junit.Assume.assumeTrue("JavaFX requires a graphical display environment", KromiumJavaFxCanvasTest.ensureJavaFx())

        val mockBrowser = mockk<KromiumBrowser>(relaxed = true)
        val root = javafx.scene.layout.StackPane()
        val textField = javafx.scene.control.TextField()
        root.children.add(textField)
        val scene = javafx.scene.Scene(root)

        KromiumJavaFxKeyboardBridge.install(scene, mockBrowser)

        val keyEvent = KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "x",
            KeyCode.X,
            false,
            false,
            false,
            false
        )

        // When focusOwner is null, bridge intercepts and sends to browser
        javafx.event.Event.fireEvent(scene, keyEvent)
        verify(atLeast = 1) { mockBrowser.view.sendKeyEvent(any()) }

        // When predicate returns false, bridge does NOT intercept
        val mockBrowser2 = mockk<KromiumBrowser>(relaxed = true)
        val scene2 = javafx.scene.Scene(javafx.scene.layout.StackPane())
        KromiumJavaFxKeyboardBridge.install(scene2, mockBrowser2) { false }
        javafx.event.Event.fireEvent(scene2, keyEvent)
        verify(exactly = 0) { mockBrowser2.view.sendKeyEvent(any()) }
    }
}
