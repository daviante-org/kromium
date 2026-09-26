package org.daviante.kromium.jcef.ui

import org.cef.browser.CefBrowser
import org.cef.callback.CefJSDialogCallback
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefJSDialogHandlerAdapter
import org.cef.misc.BoolRef
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumJsDialog
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumJsDialogType
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.jcef.core.JcefKromiumClient

internal class JcefJSDialogAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefJSDialogHandlerAdapter() {

    override fun onJSDialog(
        browser: CefBrowser?,
        origin_url: String?,
        dialog_type: CefJSDialogHandler.JSDialogType?,
        message_text: String?,
        default_prompt_text: String?,
        callback: CefJSDialogCallback?,
        suppress_message: BoolRef?
    ): Boolean {
        val listener: KromiumJsDialogListener = client.jsDialogListener ?: config.jsDialogListener ?: return false

        val mappedType = when (dialog_type) {
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT -> KromiumJsDialogType.ALERT
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM -> KromiumJsDialogType.CONFIRM
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT -> KromiumJsDialogType.PROMPT
            else -> KromiumJsDialogType.ALERT
        }

        val dialog = KromiumJsDialog(
            message = message_text ?: "",
            defaultPromptText = default_prompt_text ?: "",
            type = mappedType,
            originUrl = origin_url ?: "",
            onConfirm = { result -> callback?.Continue(true, result) },
            onCancel = { callback?.Continue(false, "") }
        )

        return try {
            val handled = listener.onDialog(dialog)
            if (!handled) {
                false
            } else {
                true
            }
        } catch (e: Throwable) {
            KromiumLogger.e(TAG, "Exception in KromiumJsDialogListener", e)
            false
        }
    }

    override fun onResetDialogState(browser: CefBrowser?) {
        // Reserved for future dialog cancellation cleanup
    }

    override fun onDialogClosed(browser: CefBrowser?) {
        // Reserved for future dialog cleanup
    }

    companion object {
        private const val TAG = "JcefJSDialogAdapter"
    }
}
