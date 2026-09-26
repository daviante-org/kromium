package org.daviante.kromium.javafx

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.ui.KromiumKeyCodes
import org.daviante.kromium.api.ui.KromiumKeyEvent
import org.daviante.kromium.api.ui.KromiumKeyEventType
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

/**
 * Universal keyboard bridge connecting JavaFX UI events directly to Kromium's core OSR keyboard engine.
 *
 * Intercepts events via a JavaFX EventFilter, translates them 1:1 to [KromiumKeyEvent],
 * dispatches directly to [KromiumBrowser.view.sendKeyEvent], and consumes the JavaFX event
 * to prevent focus stealing (Tab) or default button triggers (Enter).
 */
object KromiumJavaFxKeyboardBridge {

    /**
     * Installs the 1:1 OSR keyboard bridge at the JavaFX [Scene] level.
     *
     * Intercepts key events across the entire window whenever focus is not
     * inside an active JavaFX text input control (like address bar or inspector tabs).
     *
     * @param scene The JavaFX Scene hosting the Kromium browser.
     * @param browser The active Kromium browser instance.
     * @param filterPredicate Optional custom predicate to determine whether an event should be routed
     *                        to the browser. Defaults to intercepting unless focus is in a [TextInputControl].
     */
    fun install(
        scene: Scene,
        browser: KromiumBrowser,
        filterPredicate: (KeyEvent) -> Boolean = { true }
    ) {
        scene.addEventFilter(KeyEvent.ANY) { fxEvent ->
            val focusOwner = scene.focusOwner
            // If user is currently focused on an editable JavaFX control (e.g. addressBar),
            // allow JavaFX to process it normally!
            if (focusOwner is TextInputControl) {
                return@addEventFilter
            }

            if (!filterPredicate(fxEvent)) {
                return@addEventFilter
            }

            val kromiumEvent = toKromiumEvent(fxEvent) ?: return@addEventFilter

            // Dispatch directly to Kromium's core OSR engine
            browser.view.sendKeyEvent(kromiumEvent)

            // Consume in JavaFX to prevent Scene from stealing focus (Tab) or triggering default buttons (Enter)
            fxEvent.consume()
        }
    }

    /**
     * Installs the 1:1 OSR keyboard bridge on the specified JavaFX [Node].
     */
    fun install(targetNode: Node, browser: KromiumBrowser) {
        targetNode.addEventFilter(KeyEvent.ANY) { fxEvent ->
            val kromiumEvent = toKromiumEvent(fxEvent) ?: return@addEventFilter

            // Dispatch directly to Kromium's core OSR engine
            browser.view.sendKeyEvent(kromiumEvent)

            // Consume in JavaFX to prevent Scene from stealing focus (Tab) or triggering default buttons (Enter)
            fxEvent.consume()
        }
    }

    /**
     * Translates a JavaFX [KeyEvent] into a unified [KromiumKeyEvent].
     */
    fun toKromiumEvent(fxEvent: KeyEvent): KromiumKeyEvent? {
        val type = when (fxEvent.eventType) {
            KeyEvent.KEY_PRESSED -> KromiumKeyEventType.PRESSED
            KeyEvent.KEY_TYPED -> KromiumKeyEventType.TYPED
            KeyEvent.KEY_RELEASED -> KromiumKeyEventType.RELEASED
            else -> return null
        }

        // Modifiers extraction
        var modifiers = 0
        if (fxEvent.isShiftDown) modifiers = modifiers or KromiumKeyEvent.SHIFT_MASK
        if (fxEvent.isControlDown) modifiers = modifiers or KromiumKeyEvent.CTRL_MASK
        if (fxEvent.isAltDown) modifiers = modifiers or KromiumKeyEvent.ALT_MASK
        if (fxEvent.isMetaDown) modifiers = modifiers or KromiumKeyEvent.META_MASK
        if (fxEvent.isAltDown && fxEvent.isControlDown) modifiers = modifiers or KromiumKeyEvent.ALT_GRAPH_MASK

        return when (type) {
            KromiumKeyEventType.TYPED -> {
                val rawChar = if (fxEvent.character.isNotEmpty()) fxEvent.character[0] else return null
                // Normalize Windows carriage return to standard newline
                val keyChar = if (rawChar == '\r') '\n' else rawChar
                KromiumKeyEvent(
                    type = KromiumKeyEventType.TYPED,
                    keyCode = KromiumKeyCodes.VK_UNDEFINED,
                    keyChar = keyChar,
                    modifiers = modifiers
                )
            }
            KromiumKeyEventType.PRESSED, KromiumKeyEventType.RELEASED -> {
                val keyCode = mapKeyCode(fxEvent.code)
                val keyChar = when {
                    keyCode == KromiumKeyCodes.VK_ENTER -> '\n'
                    keyCode == KromiumKeyCodes.VK_BACK_SPACE -> '\b'
                    keyCode == KromiumKeyCodes.VK_TAB -> '\t'
                    keyCode == KromiumKeyCodes.VK_ESCAPE -> 27.toChar()
                    keyCode == KromiumKeyCodes.VK_DELETE -> 127.toChar()
                    fxEvent.text.length == 1 -> fxEvent.text[0]
                    else -> KromiumKeyEvent.CHAR_UNDEFINED
                }
                KromiumKeyEvent(
                    type = type,
                    keyCode = keyCode,
                    keyChar = keyChar,
                    modifiers = modifiers
                )
            }
        }
    }

    /**
     * 1:1 mapping table from JavaFX [KeyCode] to standard Virtual Key Codes.
     */
    fun mapKeyCode(fxCode: KeyCode): Int = when (fxCode) {
        KeyCode.ENTER -> KromiumKeyCodes.VK_ENTER
        KeyCode.BACK_SPACE -> KromiumKeyCodes.VK_BACK_SPACE
        KeyCode.TAB -> KromiumKeyCodes.VK_TAB
        KeyCode.CANCEL -> KromiumKeyCodes.VK_CANCEL
        KeyCode.CLEAR -> KromiumKeyCodes.VK_CLEAR
        KeyCode.SHIFT -> KromiumKeyCodes.VK_SHIFT
        KeyCode.CONTROL -> KromiumKeyCodes.VK_CONTROL
        KeyCode.ALT -> KromiumKeyCodes.VK_ALT
        KeyCode.PAUSE -> KromiumKeyCodes.VK_PAUSE
        KeyCode.CAPS -> KromiumKeyCodes.VK_CAPS_LOCK
        KeyCode.ESCAPE -> KromiumKeyCodes.VK_ESCAPE
        KeyCode.SPACE -> KromiumKeyCodes.VK_SPACE
        KeyCode.PAGE_UP -> KromiumKeyCodes.VK_PAGE_UP
        KeyCode.PAGE_DOWN -> KromiumKeyCodes.VK_PAGE_DOWN
        KeyCode.END -> KromiumKeyCodes.VK_END
        KeyCode.HOME -> KromiumKeyCodes.VK_HOME
        KeyCode.LEFT -> KromiumKeyCodes.VK_LEFT
        KeyCode.UP -> KromiumKeyCodes.VK_UP
        KeyCode.RIGHT -> KromiumKeyCodes.VK_RIGHT
        KeyCode.DOWN -> KromiumKeyCodes.VK_DOWN
        KeyCode.COMMA -> KromiumKeyCodes.VK_COMMA
        KeyCode.MINUS -> KromiumKeyCodes.VK_MINUS
        KeyCode.PERIOD -> KromiumKeyCodes.VK_PERIOD
        KeyCode.SLASH -> KromiumKeyCodes.VK_SLASH
        KeyCode.SEMICOLON -> KromiumKeyCodes.VK_SEMICOLON
        KeyCode.EQUALS -> KromiumKeyCodes.VK_EQUALS
        KeyCode.OPEN_BRACKET -> KromiumKeyCodes.VK_OPEN_BRACKET
        KeyCode.BACK_SLASH -> KromiumKeyCodes.VK_BACK_SLASH
        KeyCode.CLOSE_BRACKET -> KromiumKeyCodes.VK_CLOSE_BRACKET
        KeyCode.MULTIPLY -> KromiumKeyCodes.VK_MULTIPLY
        KeyCode.ADD -> KromiumKeyCodes.VK_ADD
        KeyCode.SEPARATOR -> KromiumKeyCodes.VK_SEPARATOR
        KeyCode.SUBTRACT -> KromiumKeyCodes.VK_SUBTRACT
        KeyCode.DECIMAL -> KromiumKeyCodes.VK_DECIMAL
        KeyCode.DIVIDE -> KromiumKeyCodes.VK_DIVIDE
        KeyCode.DELETE -> KromiumKeyCodes.VK_DELETE
        KeyCode.NUM_LOCK -> KromiumKeyCodes.VK_NUM_LOCK
        KeyCode.SCROLL_LOCK -> KromiumKeyCodes.VK_SCROLL_LOCK
        KeyCode.PRINTSCREEN -> KromiumKeyCodes.VK_PRINTSCREEN
        KeyCode.INSERT -> KromiumKeyCodes.VK_INSERT
        KeyCode.HELP -> KromiumKeyCodes.VK_HELP
        KeyCode.META, KeyCode.COMMAND -> KromiumKeyCodes.VK_META
        KeyCode.BACK_QUOTE -> KromiumKeyCodes.VK_BACK_QUOTE
        KeyCode.QUOTE -> KromiumKeyCodes.VK_QUOTE
        KeyCode.KP_UP -> KromiumKeyCodes.VK_KP_UP
        KeyCode.KP_DOWN -> KromiumKeyCodes.VK_KP_DOWN
        KeyCode.KP_LEFT -> KromiumKeyCodes.VK_KP_LEFT
        KeyCode.KP_RIGHT -> KromiumKeyCodes.VK_KP_RIGHT
        KeyCode.CONTEXT_MENU -> KromiumKeyCodes.VK_CONTEXT_MENU

        // Digits
        KeyCode.DIGIT0 -> KromiumKeyCodes.VK_0
        KeyCode.DIGIT1 -> KromiumKeyCodes.VK_1
        KeyCode.DIGIT2 -> KromiumKeyCodes.VK_2
        KeyCode.DIGIT3 -> KromiumKeyCodes.VK_3
        KeyCode.DIGIT4 -> KromiumKeyCodes.VK_4
        KeyCode.DIGIT5 -> KromiumKeyCodes.VK_5
        KeyCode.DIGIT6 -> KromiumKeyCodes.VK_6
        KeyCode.DIGIT7 -> KromiumKeyCodes.VK_7
        KeyCode.DIGIT8 -> KromiumKeyCodes.VK_8
        KeyCode.DIGIT9 -> KromiumKeyCodes.VK_9

        // Letters
        KeyCode.A -> KromiumKeyCodes.VK_A
        KeyCode.B -> KromiumKeyCodes.VK_B
        KeyCode.C -> KromiumKeyCodes.VK_C
        KeyCode.D -> KromiumKeyCodes.VK_D
        KeyCode.E -> KromiumKeyCodes.VK_E
        KeyCode.F -> KromiumKeyCodes.VK_F
        KeyCode.G -> KromiumKeyCodes.VK_G
        KeyCode.H -> KromiumKeyCodes.VK_H
        KeyCode.I -> KromiumKeyCodes.VK_I
        KeyCode.J -> KromiumKeyCodes.VK_J
        KeyCode.K -> KromiumKeyCodes.VK_K
        KeyCode.L -> KromiumKeyCodes.VK_L
        KeyCode.M -> KromiumKeyCodes.VK_M
        KeyCode.N -> KromiumKeyCodes.VK_N
        KeyCode.O -> KromiumKeyCodes.VK_O
        KeyCode.P -> KromiumKeyCodes.VK_P
        KeyCode.Q -> KromiumKeyCodes.VK_Q
        KeyCode.R -> KromiumKeyCodes.VK_R
        KeyCode.S -> KromiumKeyCodes.VK_S
        KeyCode.T -> KromiumKeyCodes.VK_T
        KeyCode.U -> KromiumKeyCodes.VK_U
        KeyCode.V -> KromiumKeyCodes.VK_V
        KeyCode.W -> KromiumKeyCodes.VK_W
        KeyCode.X -> KromiumKeyCodes.VK_X
        KeyCode.Y -> KromiumKeyCodes.VK_Y
        KeyCode.Z -> KromiumKeyCodes.VK_Z

        // Numpad
        KeyCode.NUMPAD0 -> KromiumKeyCodes.VK_NUMPAD0
        KeyCode.NUMPAD1 -> KromiumKeyCodes.VK_NUMPAD1
        KeyCode.NUMPAD2 -> KromiumKeyCodes.VK_NUMPAD2
        KeyCode.NUMPAD3 -> KromiumKeyCodes.VK_NUMPAD3
        KeyCode.NUMPAD4 -> KromiumKeyCodes.VK_NUMPAD4
        KeyCode.NUMPAD5 -> KromiumKeyCodes.VK_NUMPAD5
        KeyCode.NUMPAD6 -> KromiumKeyCodes.VK_NUMPAD6
        KeyCode.NUMPAD7 -> KromiumKeyCodes.VK_NUMPAD7
        KeyCode.NUMPAD8 -> KromiumKeyCodes.VK_NUMPAD8
        KeyCode.NUMPAD9 -> KromiumKeyCodes.VK_NUMPAD9

        // Function keys
        KeyCode.F1 -> KromiumKeyCodes.VK_F1
        KeyCode.F2 -> KromiumKeyCodes.VK_F2
        KeyCode.F3 -> KromiumKeyCodes.VK_F3
        KeyCode.F4 -> KromiumKeyCodes.VK_F4
        KeyCode.F5 -> KromiumKeyCodes.VK_F5
        KeyCode.F6 -> KromiumKeyCodes.VK_F6
        KeyCode.F7 -> KromiumKeyCodes.VK_F7
        KeyCode.F8 -> KromiumKeyCodes.VK_F8
        KeyCode.F9 -> KromiumKeyCodes.VK_F9
        KeyCode.F10 -> KromiumKeyCodes.VK_F10
        KeyCode.F11 -> KromiumKeyCodes.VK_F11
        KeyCode.F12 -> KromiumKeyCodes.VK_F12

        // Fallback to integer code
        else -> fxCode.code
    }
}

/**
 * Attaches Kromium's 1:1 OSR keyboard bridge to this JavaFX [Node],
 * forwarding all keyboard events directly to this [KromiumBrowser] instance.
 */
fun KromiumBrowser.attachJavaFxKeyboard(targetNode: Node) {
    KromiumJavaFxKeyboardBridge.install(targetNode, this)
}

/**
 * Attaches Kromium's 1:1 OSR keyboard bridge to this JavaFX [Node],
 * forwarding all keyboard events directly to the specified [KromiumBrowser].
 */
fun Node.attachKromiumKeyboard(browser: KromiumBrowser) {
    KromiumJavaFxKeyboardBridge.install(this, browser)
}
