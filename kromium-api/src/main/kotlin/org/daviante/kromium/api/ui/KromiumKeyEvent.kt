package org.daviante.kromium.api.ui

/**
 * Standard event types for keyboard input across all UI toolkits.
 */
enum class KromiumKeyEventType {
    PRESSED,
    TYPED,
    RELEASED
}

/**
 * Framework-independent model representing a keyboard event in Kromium.
 *
 * @property type Whether this is a key down, key character typed, or key up.
 * @property keyCode Standard virtual key code (see [KromiumKeyCodes]).
 * @property keyChar Character value for printable input and text composition.
 * @property modifiers Bitmask of active modifier keys (see constants on companion object).
 */
data class KromiumKeyEvent(
    val type: KromiumKeyEventType,
    val keyCode: Int = 0,
    val keyChar: Char = CHAR_UNDEFINED,
    val modifiers: Int = 0
) {
    val isShiftDown: Boolean get() = (modifiers and SHIFT_MASK) != 0
    val isControlDown: Boolean get() = (modifiers and CTRL_MASK) != 0
    val isAltDown: Boolean get() = (modifiers and ALT_MASK) != 0
    val isMetaDown: Boolean get() = (modifiers and META_MASK) != 0
    val isAltGraphDown: Boolean get() = (modifiers and ALT_GRAPH_MASK) != 0

    companion object {
        const val CHAR_UNDEFINED: Char = '\uFFFF'

        const val SHIFT_MASK: Int = 1 shl 0
        const val CTRL_MASK: Int = 1 shl 1
        const val ALT_MASK: Int = 1 shl 2
        const val META_MASK: Int = 1 shl 3
        const val ALT_GRAPH_MASK: Int = 1 shl 4
    }
}
