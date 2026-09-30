package org.daviante.kromium.api.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KromiumJsDialogTest {

    @Test
    fun testConfirmInvokesOnConfirmWithPromptResult() {
        var confirmedResult: String? = null
        var cancelInvoked = false

        val dialog = KromiumJsDialog(
            message = "Enter your name",
            defaultPromptText = "Alice",
            type = KromiumJsDialogType.PROMPT,
            originUrl = "https://example.com",
            onConfirm = { result -> confirmedResult = result },
            onCancel = { cancelInvoked = true }
        )

        assertEquals("Enter your name", dialog.message)
        assertEquals("Alice", dialog.defaultPromptText)
        assertEquals(KromiumJsDialogType.PROMPT, dialog.type)
        assertEquals("https://example.com", dialog.originUrl)
        assertFalse(dialog.isHandled)

        dialog.confirm("Bob")
        assertTrue(dialog.isHandled)
        assertEquals("Bob", confirmedResult)
        assertFalse(cancelInvoked)
    }

    @Test
    fun testConfirmDefaultPrompt() {
        var confirmedResult: String? = null

        val dialog = KromiumJsDialog(
            message = "Confirm default",
            defaultPromptText = "DefaultText",
            type = KromiumJsDialogType.PROMPT,
            onConfirm = { result -> confirmedResult = result },
            onCancel = {}
        )

        dialog.confirm()
        assertTrue(dialog.isHandled)
        assertEquals("DefaultText", confirmedResult)
    }

    @Test
    fun testCancelInvokesOnCancel() {
        var confirmInvoked = false
        var cancelInvoked = false

        val dialog = KromiumJsDialog(
            message = "Are you sure?",
            defaultPromptText = "",
            type = KromiumJsDialogType.CONFIRM,
            onConfirm = { confirmInvoked = true },
            onCancel = { cancelInvoked = true }
        )

        assertFalse(dialog.isHandled)
        dialog.cancel()
        assertTrue(dialog.isHandled)
        assertTrue(cancelInvoked)
        assertFalse(confirmInvoked)
    }

    @Test
    fun testAtomicExecutionPreventsDoubleInvocations() {
        var confirmCount = 0
        var cancelCount = 0

        val dialog = KromiumJsDialog(
            message = "Modal alert",
            defaultPromptText = "",
            type = KromiumJsDialogType.ALERT,
            onConfirm = { confirmCount++ },
            onCancel = { cancelCount++ }
        )

        dialog.confirm()
        dialog.confirm()
        dialog.cancel()

        assertEquals(1, confirmCount)
        assertEquals(0, cancelCount)
        assertTrue(dialog.isHandled)
    }
}
