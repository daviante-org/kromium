package org.daviante.kromium.api.ui

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Encapsulates a JavaScript alert, confirm, or prompt dialog with thread-safe continuation.
 *
 * @property message The text message displayed by the dialog.
 * @property defaultPromptText The default input text if [type] is [KromiumJsDialogType.PROMPT].
 * @property type The dialog classification ([KromiumJsDialogType.ALERT], [KromiumJsDialogType.CONFIRM], [KromiumJsDialogType.PROMPT]).
 * @property originUrl The URL of the web page that initiated the dialog.
 */
class KromiumJsDialog(
    val message: String,
    val defaultPromptText: String,
    val type: KromiumJsDialogType,
    val originUrl: String = "",
    private val onConfirm: (promptResult: String) -> Unit,
    private val onCancel: () -> Unit
) {
    private val handled = AtomicBoolean(false)

    /**
     * Confirms or accepts the dialog.
     * For prompts, passes [promptResult] back to the JavaScript engine.
     */
    fun confirm(promptResult: String = defaultPromptText) {
        if (handled.compareAndSet(false, true)) {
            onConfirm(promptResult)
        }
    }

    /**
     * Cancels or dismisses the dialog.
     */
    fun cancel() {
        if (handled.compareAndSet(false, true)) {
            onCancel()
        }
    }

    /**
     * Indicates whether this dialog has already been confirmed or cancelled.
     */
    val isHandled: Boolean
        get() = handled.get()
}
