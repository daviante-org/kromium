package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumKeyEventType
import org.cef.browser.CefBrowser
import sun.misc.Unsafe
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/**
 * Universal cross-platform OSR keyboard dispatcher for Kromium's JCEF implementation.
 *
 * Translates framework-independent [KromiumKeyEvent] instances into native Chromium
 * RawKeyDown, Char, and KeyUp sequences while executing application-level shortcuts
 * (clipboard, history, zoom, reload) on both macOS (Command) and Windows/Linux (Control).
 *
 * Populates native Windows rawCode and hardware scancodes via [sun.misc.Unsafe] so that
 * JCEF's native SendKeyEvent bridge (`CefBrowser_N.cpp`) correctly populates Chromium's
 * `windows_key_code` and `native_key_code`.
 */
object JcefKeyboardDispatcher {

    private val fallbackComponent: Component = object : Component() {}

    private val unsafe: Unsafe? = try {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as? Unsafe
    } catch (_: Throwable) {
        null
    }

    private val rawCodeOffset: Long = try {
        val field = KeyEvent::class.java.getDeclaredField("rawCode")
        unsafe?.objectFieldOffset(field) ?: -1L
    } catch (_: Throwable) {
        -1L
    }

    private val scancodeOffset: Long = try {
        val field = KeyEvent::class.java.getDeclaredField("scancode")
        unsafe?.objectFieldOffset(field) ?: -1L
    } catch (_: Throwable) {
        -1L
    }

    private val extendedKeyCodeOffset: Long = try {
        val field = KeyEvent::class.java.getDeclaredField("extendedKeyCode")
        unsafe?.objectFieldOffset(field) ?: -1L
    } catch (_: Throwable) {
        -1L
    }

    /**
     * Dispatches a [KromiumKeyEvent] to the given [CefBrowser].
     *
     * @param browser The active Chromium browser instance.
     * @param event The unified keyboard event to dispatch.
     * @return true if the event was handled (as a shortcut or forwarded to Chromium).
     */
    fun dispatch(browser: CefBrowser, event: KromiumKeyEvent): Boolean {
        // Ensure browser host is focused so Chromium never discards key events
        browser.setFocus(true)

        val awtEvent = toAwtKeyEvent(browser, event)

        // 1. Check if this is an application/browser shortcut (e.g. Cmd+C, Ctrl+V, F5, Ctrl+Z, Zoom)
        if (JcefShortcutHandler.handleAwtKeyEvent(browser, awtEvent)) {
            return true
        }

        // 2. Forward directly to native CEF SendKeyEvent
        browser.sendKeyEvent(awtEvent)
        return true
    }

    /**
     * Converts a [KromiumKeyEvent] into an AWT [KeyEvent] compatible with JCEF's native SendKeyEvent bridge.
     */
    fun toAwtKeyEvent(browser: CefBrowser, event: KromiumKeyEvent): KeyEvent {
        val source = browser.uiComponent ?: fallbackComponent
        val id = when (event.type) {
            KromiumKeyEventType.PRESSED -> KeyEvent.KEY_PRESSED
            KromiumKeyEventType.TYPED -> KeyEvent.KEY_TYPED
            KromiumKeyEventType.RELEASED -> KeyEvent.KEY_RELEASED
        }

        val whenTime = System.currentTimeMillis()
        var modifiers = 0
        if (event.isShiftDown) modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        if (event.isControlDown) modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        if (event.isAltDown) modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        if (event.isMetaDown) modifiers = modifiers or InputEvent.META_DOWN_MASK
        if (event.isAltGraphDown) modifiers = modifiers or InputEvent.ALT_GRAPH_DOWN_MASK

        // Determine virtual key code
        val keyCode = if (event.type == KromiumKeyEventType.TYPED) {
            KeyEvent.VK_UNDEFINED
        } else {
            event.keyCode
        }

        // Determine character
        val keyChar = when {
            event.keyChar != KromiumKeyEvent.CHAR_UNDEFINED -> event.keyChar
            keyCode == KromiumKeyCodes.VK_ENTER -> '\n'
            keyCode == KromiumKeyCodes.VK_BACK_SPACE -> '\b'
            keyCode == KromiumKeyCodes.VK_TAB -> '\t'
            keyCode == KromiumKeyCodes.VK_ESCAPE -> 27.toChar()
            keyCode == KromiumKeyCodes.VK_DELETE -> 127.toChar()
            keyCode in KromiumKeyCodes.VK_A..KromiumKeyCodes.VK_Z -> {
                val c = (keyCode - KromiumKeyCodes.VK_A + 'a'.code).toChar()
                if (event.isShiftDown) c.uppercaseChar() else c
            }
            keyCode in KromiumKeyCodes.VK_0..KromiumKeyCodes.VK_9 -> {
                (keyCode - KromiumKeyCodes.VK_0 + '0'.code).toChar()
            }
            keyCode == KromiumKeyCodes.VK_SPACE -> ' '
            keyCode == KromiumKeyCodes.VK_COMMA -> if (event.isShiftDown) '<' else ','
            keyCode == KromiumKeyCodes.VK_PERIOD -> if (event.isShiftDown) '>' else '.'
            keyCode == KromiumKeyCodes.VK_SLASH -> if (event.isShiftDown) '?' else '/'
            keyCode == KromiumKeyCodes.VK_SEMICOLON -> if (event.isShiftDown) ':' else ';'
            keyCode == KromiumKeyCodes.VK_QUOTE -> if (event.isShiftDown) '"' else '\''
            keyCode == KromiumKeyCodes.VK_OPEN_BRACKET -> if (event.isShiftDown) '{' else '['
            keyCode == KromiumKeyCodes.VK_CLOSE_BRACKET -> if (event.isShiftDown) '}' else ']'
            keyCode == KromiumKeyCodes.VK_BACK_SLASH -> if (event.isShiftDown) '|' else '\\'
            keyCode == KromiumKeyCodes.VK_MINUS -> if (event.isShiftDown) '_' else '-'
            keyCode == KromiumKeyCodes.VK_EQUALS -> if (event.isShiftDown) '+' else '='
            keyCode == KromiumKeyCodes.VK_BACK_QUOTE -> if (event.isShiftDown) '~' else '`'
            else -> KeyEvent.CHAR_UNDEFINED
        }

        val awtEvent = KeyEvent(source, id, whenTime, modifiers, keyCode, keyChar)

        // Populate rawCode, scancode, and extendedKeyCode required by native jcef.dll
        val u = unsafe
        if (u != null) {
            val rawCode = mapToWindowsRawCode(event, keyCode, keyChar)
            val scancode = mapToScanCode(keyCode)
            if (rawCodeOffset >= 0) u.putLong(awtEvent, rawCodeOffset, rawCode)
            if (scancodeOffset >= 0) u.putLong(awtEvent, scancodeOffset, scancode)
            if (extendedKeyCodeOffset >= 0) u.putLong(awtEvent, extendedKeyCodeOffset, keyCode.toLong())
        }

        return awtEvent
    }

    /**
     * Maps virtual key codes to Windows native virtual key codes (e.g. VK_RETURN = 13).
     */
    fun mapToWindowsRawCode(event: KromiumKeyEvent, keyCode: Int, keyChar: Char): Long = when {
        event.type == KromiumKeyEventType.TYPED -> keyChar.code.toLong()
        keyCode == KromiumKeyCodes.VK_ENTER -> 13L // Windows VK_RETURN = 0x0D (AWT VK_ENTER is 10)
        else -> keyCode.toLong()
    }

    /**
     * Maps standard Virtual Key Codes to standard PC/AT hardware scan codes.
     */
    fun mapToScanCode(keyCode: Int): Long = when (keyCode) {
        KromiumKeyCodes.VK_ESCAPE -> 1L
        KromiumKeyCodes.VK_1 -> 2L
        KromiumKeyCodes.VK_2 -> 3L
        KromiumKeyCodes.VK_3 -> 4L
        KromiumKeyCodes.VK_4 -> 5L
        KromiumKeyCodes.VK_5 -> 6L
        KromiumKeyCodes.VK_6 -> 7L
        KromiumKeyCodes.VK_7 -> 8L
        KromiumKeyCodes.VK_8 -> 9L
        KromiumKeyCodes.VK_9 -> 10L
        KromiumKeyCodes.VK_0 -> 11L
        KromiumKeyCodes.VK_MINUS -> 12L
        KromiumKeyCodes.VK_EQUALS -> 13L
        KromiumKeyCodes.VK_BACK_SPACE -> 14L
        KromiumKeyCodes.VK_TAB -> 15L
        KromiumKeyCodes.VK_Q -> 16L
        KromiumKeyCodes.VK_W -> 17L
        KromiumKeyCodes.VK_E -> 18L
        KromiumKeyCodes.VK_R -> 19L
        KromiumKeyCodes.VK_T -> 20L
        KromiumKeyCodes.VK_Y -> 21L
        KromiumKeyCodes.VK_U -> 22L
        KromiumKeyCodes.VK_I -> 23L
        KromiumKeyCodes.VK_O -> 24L
        KromiumKeyCodes.VK_P -> 25L
        KromiumKeyCodes.VK_OPEN_BRACKET -> 26L
        KromiumKeyCodes.VK_CLOSE_BRACKET -> 27L
        KromiumKeyCodes.VK_ENTER -> 28L
        KromiumKeyCodes.VK_CONTROL -> 29L
        KromiumKeyCodes.VK_A -> 30L
        KromiumKeyCodes.VK_S -> 31L
        KromiumKeyCodes.VK_D -> 32L
        KromiumKeyCodes.VK_F -> 33L
        KromiumKeyCodes.VK_G -> 34L
        KromiumKeyCodes.VK_H -> 35L
        KromiumKeyCodes.VK_J -> 36L
        KromiumKeyCodes.VK_K -> 37L
        KromiumKeyCodes.VK_L -> 38L
        KromiumKeyCodes.VK_SEMICOLON -> 39L
        KromiumKeyCodes.VK_QUOTE -> 40L
        KromiumKeyCodes.VK_BACK_QUOTE -> 41L
        KromiumKeyCodes.VK_SHIFT -> 42L
        KromiumKeyCodes.VK_BACK_SLASH -> 43L
        KromiumKeyCodes.VK_Z -> 44L
        KromiumKeyCodes.VK_X -> 45L
        KromiumKeyCodes.VK_C -> 46L
        KromiumKeyCodes.VK_V -> 47L
        KromiumKeyCodes.VK_B -> 48L
        KromiumKeyCodes.VK_N -> 49L
        KromiumKeyCodes.VK_M -> 50L
        KromiumKeyCodes.VK_COMMA -> 51L
        KromiumKeyCodes.VK_PERIOD -> 52L
        KromiumKeyCodes.VK_SLASH -> 53L
        KromiumKeyCodes.VK_ALT -> 56L
        KromiumKeyCodes.VK_SPACE -> 57L
        KromiumKeyCodes.VK_CAPS_LOCK -> 58L
        KromiumKeyCodes.VK_UP -> 0xE048L
        KromiumKeyCodes.VK_LEFT -> 0xE04BL
        KromiumKeyCodes.VK_RIGHT -> 0xE04DL
        KromiumKeyCodes.VK_DOWN -> 0xE050L
        KromiumKeyCodes.VK_DELETE -> 0xE053L
        KromiumKeyCodes.VK_HOME -> 0xE047L
        KromiumKeyCodes.VK_END -> 0xE04FL
        KromiumKeyCodes.VK_PAGE_UP -> 0xE049L
        KromiumKeyCodes.VK_PAGE_DOWN -> 0xE051L
        else -> 0L
    }
}
