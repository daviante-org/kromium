package org.daviante.kromium.api.automation

/**
 * Facade for clipboard and editing operations.
 */
interface KromiumClipboard {
    /** Copies the current selection to the clipboard. */
    fun copy()

    /** Pastes the clipboard content into the current focused element. */
    fun paste()

    /** Cuts the current selection to the clipboard. */
    fun cut()

    /** Selects all text/content in the current focused element. */
    fun selectAll()

    /** Undoes the last editing action. */
    fun undo()

    /** Redoes the last undone editing action. */
    fun redo()
}
