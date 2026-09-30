package org.daviante.kromium.api.ui

/**
 * Listener for handling JavaScript dialogs (alert, confirm, prompt).
 */
fun interface KromiumJsDialogListener {
    /**
     * Called when a webpage triggers an alert, confirm, or prompt dialog.
     *
     * @param dialog Encapsulation of the dialog request with confirm/cancel actions.
     * @return `true` if the host application is handling the dialog, or `false` to let Chromium use default handling.
     */
    fun onDialog(dialog: KromiumJsDialog): Boolean
}
