package org.daviante.kromium.swt

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumKeyEventType
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Event

/**
 * Cross-platform keyboard bridge connecting Eclipse SWT UI events
 * directly to Kromium's core OSR keyboard engine.
 */
object KromiumSwtKeyboardBridge {

    val isMac: Boolean = System.getProperty("os.name", "").lowercase().contains("mac")

    /**
     * Whether keyboard input should currently route to the Chromium browser view.
     * Default to true so typing immediately works upon view presentation.
     */
    @Volatile
    var isBrowserActive: Boolean = true

    /**
     * Explicitly sets whether the browser view has active keyboard focus.
     */
    fun setBrowserFocused(focused: Boolean) {
        isBrowserActive = focused
    }

    fun mapSwtModifiers(stateMask: Int): Int {
        var modifiers = 0
        if ((stateMask and SWT.SHIFT) != 0) modifiers = modifiers or KromiumKeyEvent.SHIFT_MASK
        if ((stateMask and SWT.CTRL) != 0) modifiers = modifiers or KromiumKeyEvent.CTRL_MASK
        if ((stateMask and SWT.ALT) != 0) modifiers = modifiers or KromiumKeyEvent.ALT_MASK
        if ((stateMask and SWT.COMMAND) != 0) modifiers = modifiers or KromiumKeyEvent.META_MASK
        return modifiers
    }

    fun dispatchSwtKeyEvent(
        browser: KromiumBrowser,
        keyCode: Int,
        character: Char,
        stateMask: Int,
        isKeyDown: Boolean
    ): Boolean {
        val modifiers = mapSwtModifiers(stateMask)
        val mappedKeyCode = mapSwtKeyCode(keyCode)
        val rawChar = character
        val effectiveChar = when {
            mappedKeyCode == KromiumKeyCodes.VK_ENTER -> '\n'
            mappedKeyCode == KromiumKeyCodes.VK_BACK_SPACE -> '\b'
            mappedKeyCode == KromiumKeyCodes.VK_TAB -> '\t'
            mappedKeyCode == KromiumKeyCodes.VK_ESCAPE -> 27.toChar()
            mappedKeyCode == KromiumKeyCodes.VK_DELETE -> 127.toChar()
            rawChar != '\u0000' -> if (rawChar == '\r') '\n' else rawChar
            mappedKeyCode in KromiumKeyCodes.VK_A..KromiumKeyCodes.VK_Z -> {
                val c = (mappedKeyCode - KromiumKeyCodes.VK_A + 'a'.code).toChar()
                if ((modifiers and KromiumKeyEvent.SHIFT_MASK) != 0) c.uppercaseChar() else c
            }
            mappedKeyCode in KromiumKeyCodes.VK_0..KromiumKeyCodes.VK_9 -> {
                (mappedKeyCode - KromiumKeyCodes.VK_0 + '0'.code).toChar()
            }
            mappedKeyCode == KromiumKeyCodes.VK_SPACE -> ' '
            else -> KromiumKeyEvent.CHAR_UNDEFINED
        }

        if (isKeyDown) {
            val pressed = KromiumKeyEvent(
                type = KromiumKeyEventType.PRESSED,
                keyCode = mappedKeyCode,
                keyChar = effectiveChar,
                modifiers = modifiers
            )
            browser.view.sendKeyEvent(pressed)

            val isPrintable = rawChar.code in 32..126 || (rawChar.code >= 160 && rawChar != '\uFFFF')
            val isShortcut = (modifiers and (KromiumKeyEvent.CTRL_MASK or KromiumKeyEvent.META_MASK)) != 0
            if (isPrintable && !isShortcut) {
                val typedChar = if (rawChar == '\r') '\n' else rawChar
                val typed = KromiumKeyEvent(
                    type = KromiumKeyEventType.TYPED,
                    keyCode = KromiumKeyCodes.VK_UNDEFINED,
                    keyChar = typedChar,
                    modifiers = modifiers
                )
                browser.view.sendKeyEvent(typed)
            }
        } else {
            val released = KromiumKeyEvent(
                type = KromiumKeyEventType.RELEASED,
                keyCode = mappedKeyCode,
                keyChar = effectiveChar,
                modifiers = modifiers
            )
            browser.view.sendKeyEvent(released)
        }

        return true
    }

    fun dispatchSwtKeyEvent(
        browser: KromiumBrowser,
        event: Event,
        isKeyDown: Boolean
    ): Boolean = dispatchSwtKeyEvent(browser, event.keyCode, event.character, event.stateMask, isKeyDown)

    fun dispatchSwtKeyEvent(
        browser: KromiumBrowser,
        event: org.eclipse.swt.events.KeyEvent,
        isKeyDown: Boolean
    ): Boolean = dispatchSwtKeyEvent(browser, event.keyCode, event.character, event.stateMask, isKeyDown)

    fun mapSwtKeyCode(swtCode: Int): Int = when (swtCode) {
        SWT.CR.code, SWT.LF.code -> KromiumKeyCodes.VK_ENTER
        SWT.BS.code -> KromiumKeyCodes.VK_BACK_SPACE
        SWT.TAB.code -> KromiumKeyCodes.VK_TAB
        SWT.ESC.code -> KromiumKeyCodes.VK_ESCAPE
        SWT.DEL.code -> KromiumKeyCodes.VK_DELETE
        SWT.ARROW_LEFT -> KromiumKeyCodes.VK_LEFT
        SWT.ARROW_RIGHT -> KromiumKeyCodes.VK_RIGHT
        SWT.ARROW_UP -> KromiumKeyCodes.VK_UP
        SWT.ARROW_DOWN -> KromiumKeyCodes.VK_DOWN
        SWT.PAGE_UP -> KromiumKeyCodes.VK_PAGE_UP
        SWT.PAGE_DOWN -> KromiumKeyCodes.VK_PAGE_DOWN
        SWT.HOME -> KromiumKeyCodes.VK_HOME
        SWT.END -> KromiumKeyCodes.VK_END
        SWT.INSERT -> KromiumKeyCodes.VK_INSERT
        SWT.F1 -> KromiumKeyCodes.VK_F1
        SWT.F2 -> KromiumKeyCodes.VK_F2
        SWT.F3 -> KromiumKeyCodes.VK_F3
        SWT.F4 -> KromiumKeyCodes.VK_F4
        SWT.F5 -> KromiumKeyCodes.VK_F5
        SWT.F6 -> KromiumKeyCodes.VK_F6
        SWT.F7 -> KromiumKeyCodes.VK_F7
        SWT.F8 -> KromiumKeyCodes.VK_F8
        SWT.F9 -> KromiumKeyCodes.VK_F9
        SWT.F10 -> KromiumKeyCodes.VK_F10
        SWT.F11 -> KromiumKeyCodes.VK_F11
        SWT.F12 -> KromiumKeyCodes.VK_F12
        SWT.SHIFT -> KromiumKeyCodes.VK_SHIFT
        SWT.CTRL -> KromiumKeyCodes.VK_CONTROL
        SWT.ALT -> KromiumKeyCodes.VK_ALT
        SWT.COMMAND -> KromiumKeyCodes.VK_META
        in 'a'.code..'z'.code -> swtCode - 32 // convert lowercase ASCII to uppercase virtual key code
        in 'A'.code..'Z'.code -> swtCode
        in '0'.code..'9'.code -> swtCode
        ' '.code -> KromiumKeyCodes.VK_SPACE
        '\''.code -> KromiumKeyCodes.VK_QUOTE
        '`'.code -> KromiumKeyCodes.VK_BACK_QUOTE
        else -> swtCode
    }
}
