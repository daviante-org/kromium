[Documentation Hub](../README.md) / Core API / **Interactive Dialogs & Context Menus**

---

# Interactive Dialogs & Context Menus API

Kromium provides a 100% provider-agnostic, thread-safe UI subsystem for intercepting JavaScript dialogs (`alert`, `confirm`, `prompt`) and constructing custom right-click context menus.

---

## 1. JavaScript Dialog Handling

When web pages call `window.alert()`, `window.confirm()`, or `window.prompt()`, Kromium intercepts the native Chromium request and wraps it in a [`KromiumJsDialog`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumJsDialog.kt) object.

In Off-Screen Rendering (OSR), unhandled dialog callbacks can cause the rendering engine to deadlock. Kromium's `KromiumJsDialog` uses thread-safe atomic guards to ensure callbacks are executed cleanly and non-blockingly.

### The Dialog Model (`KromiumJsDialog`)

| Property / Method | Description |
| :--- | :--- |
| `message: String` | The text message passed by the web page. |
| `defaultPromptText: String` | Default text provided if the dialog is a prompt. |
| `type: KromiumJsDialogType` | Dialog type: `ALERT`, `CONFIRM`, or `PROMPT`. |
| `originUrl: String` | The URL of the page that triggered the dialog. |
| `confirm(promptResult: String)` | Confirms/accepts the dialog, passing user input for prompts. |
| `cancel()` | Cancels/dismisses the dialog. |
| `isHandled: Boolean` | `true` if `confirm()` or `cancel()` has already been invoked. |

### Registering a Dialog Listener

Attach a [`KromiumJsDialogListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumJsDialogListener.kt) to the `KromiumClient`:

```kotlin
import org.daviante.kromium.api.ui.KromiumJsDialogListener
import org.daviante.kromium.api.ui.KromiumJsDialogType

client.jsDialogListener = KromiumJsDialogListener { dialog ->
    when (dialog.type) {
        KromiumJsDialogType.ALERT -> {
            println("[Web Alert from ${dialog.originUrl}]: ${dialog.message}")
            dialog.confirm() // Auto-dismiss alert
            true // Handled by host application
        }

        KromiumJsDialogType.CONFIRM -> {
            // For headless / automated environments, confirm or cancel programmatically:
            println("[Web Confirm]: ${dialog.message}")
            dialog.confirm()
            true
        }

        KromiumJsDialogType.PROMPT -> {
            println("[Web Prompt]: ${dialog.message}")
            // Provide response back to JavaScript:
            dialog.confirm(promptResult = "Automated Response")
            true
        }
    }
}
```

> **Note:** Returning `true` signals that your application handled the dialog. If you return `false` or unregister the listener, Kromium automatically safely dismisses the dialog with default parameters, preventing any engine freeze.

### Desktop Dialog UI Integration (Swing Example)

```kotlin
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

client.jsDialogListener = KromiumJsDialogListener { dialog ->
    SwingUtilities.invokeLater {
        when (dialog.type) {
            KromiumJsDialogType.ALERT -> {
                JOptionPane.showMessageDialog(null, dialog.message, "Page Alert", JOptionPane.INFORMATION_MESSAGE)
                dialog.confirm()
            }
            KromiumJsDialogType.CONFIRM -> {
                val res = JOptionPane.showConfirmDialog(null, dialog.message, "Confirm", JOptionPane.YES_NO_OPTION)
                if (res == JOptionPane.YES_OPTION) dialog.confirm() else dialog.cancel()
            }
            KromiumJsDialogType.PROMPT -> {
                val input = JOptionPane.showInputDialog(null, dialog.message, dialog.defaultPromptText)
                if (input != null) dialog.confirm(input) else dialog.cancel()
            }
        }
    }
    true
}
```

---

## 2. Custom Right-Click Context Menus

Kromium allows you to completely customize, replace, or suppress context menus using the declarative [`KromiumMenuBuilder`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumMenuBuilder.kt).

### Out-of-the-Box Presets

For common use cases, [`KromiumContextMenuListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumContextMenuListener.kt) provides ready-to-use factory presets:

```kotlin
import org.daviante.kromium.api.ui.KromiumContextMenuListener

// 1. Disable right-click menus entirely
client.contextMenuListener = KromiumContextMenuListener.disabled()

// 2. Default browser context menu
client.contextMenuListener = KromiumContextMenuListener.defaultMenu()

// 3. Developer mode only (Inspect Element)
client.contextMenuListener = KromiumContextMenuListener.devToolsOnly()

// 4. Minimal desktop editing menu (Cut, Copy, Paste, Search Web, Copy Link)
client.contextMenuListener = KromiumContextMenuListener.minimalEditing(includeInspectElement = true)
```

---

## 3. Building Custom Context Menus with Kotlin DSL

To create custom contextual menus, implement the `KromiumContextMenuListener` lambda:

```kotlin
client.contextMenuListener = KromiumContextMenuListener { builder, context ->
    // 1. Clear Chromium's default browser items:
    builder.clear()

    // 2. Add custom contextual actions:
    if (context.params.isLink()) {
        builder.item("Open in External Browser") { ctx ->
            ctx.params.linkUrl?.let { url ->
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url))
            }
        }
        builder.copyLink("Copy Link Address")
        builder.separator()
    }

    if (context.params.hasSelection()) {
        builder.item("Quote in Editor") { ctx ->
            println("Selected quote: ${ctx.params.selectionText}")
        }
        builder.searchWeb() // Turnkey: searches Google for selected text
        builder.copy()      // Native Copy command
        builder.separator()
    }

    if (context.params.isEditable) {
        builder.cut()
        builder.paste()
        builder.selectAll()
        builder.separator()
    }

    // 3. Add checkboxes, nested submenus, and radio buttons:
    builder.subMenu("View Options") {
        checkItem("Dark Mode", checked = true) { isChecked ->
            println("Toggled dark mode: $isChecked")
        }
        separator()
        radioItem("100% Zoom", checked = true, groupId = 1) {
            context.browser?.view?.setZoom(0.0)
        }
        radioItem("120% Zoom", checked = false, groupId = 1) {
            context.browser?.view?.setZoom(1.0)
        }
    }

    builder.separator()

    // 4. Built-in Inspect Element at clicked coordinate
    builder.inspectElement()
}
```

---

## 4. Context Parameters & Helper Actions

When the context menu is built or an item is clicked, [`KromiumContextMenuContext`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumContextMenuContext.kt) and [`KromiumContextMenuParams`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/ui/KromiumContextMenuParams.kt) give you full inspection capabilities:

### Parameter Snapshot (`context.params`)

- `x: Int`, `y: Int`: Viewport coordinates of the right-click.
- `linkUrl: String?`: Hyperlink target URL under the cursor.
- `sourceUrl: String?`: Source URL of clicked image, video, or audio media.
- `selectionText: String?`: Currently highlighted text.
- `isEditable: Boolean`: Whether the click occurred inside an input field or contenteditable element.
- `isLink(): Boolean`: Helper returning true if clicked on a hyperlink.
- `hasMedia(): Boolean`: Helper returning true if clicked on an image or media element.
- `hasSelection(): Boolean`: Helper returning true if text is currently highlighted.

### Turnkey Action Helpers (`context`)

- `context.copyToClipboard(text)`: Safely copies text to the operating system clipboard.
- `context.inspectElement()`: Opens Chrome DevTools targeting the clicked coordinate.
- `context.startDownload(url)`: Enqueues a file download through Kromium's download pipeline.
- `context.searchWeb(query, engineUrl, openInSystemBrowser)`: Executes web searches with configurable search engine templates.

---

## Navigation

- [← Previous: Web Automation Engine](automation.md)
- [Home: Documentation Hub](../README.md)
- [Next: Enterprise Networking & Proxies →](network-and-proxies.md)
