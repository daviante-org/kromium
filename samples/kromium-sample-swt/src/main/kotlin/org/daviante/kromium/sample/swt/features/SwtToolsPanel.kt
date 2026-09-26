package org.daviante.kromium.sample.swt.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swt.ui.SwtTheme
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

class SwtToolsPanel(
    parent: Composite,
    private var browser: KromiumBrowser?,
    private val theme: SwtTheme
) : Composite(parent, SWT.NONE) {

    private val statusLabel: Label
    private val cookieTable: Table

    fun setBrowser(b: KromiumBrowser) {
        this.browser = b
    }

    init {
        layout = GridLayout(2, false)
        layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        background = theme.surface

        // Left Column: Page Capture & Printing
        val leftGroup = Group(this, SWT.NONE).apply {
            text = "Page Capture & Export"
            foreground = theme.textPrimary
            background = theme.surface
            layout = GridLayout(1, false)
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            font = theme.bodyFont
        }

        Button(leftGroup, SWT.PUSH).apply {
            text = "📷 Capture High-Res Screenshot"
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    captureScreenshot()
                }
            })
        }

        Button(leftGroup, SWT.PUSH).apply {
            text = "📄 Export Page to Vector PDF"
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    exportPdf()
                }
            })
        }

        Label(leftGroup, SWT.SEPARATOR or SWT.HORIZONTAL).apply {
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        }

        Button(leftGroup, SWT.PUSH).apply {
            text = "🗑 Clear All Cookies"
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val b = browser ?: return
                    val cleared = b.storage.clearCookies()
                    statusLabel.text = if (cleared) "Cookies cleared successfully" else "Failed to clear cookies"
                    statusLabel.foreground = if (cleared) theme.success else theme.error
                    loadCookies()
                }
            })
        }

        Button(leftGroup, SWT.PUSH).apply {
            text = "🧹 Clear Local & Session Storage"
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val b = browser ?: return
                    b.storage.clearWebStorageAsync().thenAccept { cleared ->
                        display.asyncExec {
                            if (!isDisposed) {
                                statusLabel.text = if (cleared) "Web storage cleared" else "Failed to clear storage"
                                statusLabel.foreground = if (cleared) theme.success else theme.error
                            }
                        }
                    }
                }
            })
        }

        statusLabel = Label(leftGroup, SWT.WRAP).apply {
            text = "Ready"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        }

        // Right Column: Cookie Inspector
        val rightGroup = Group(this, SWT.NONE).apply {
            text = "Cookies Inspector"
            foreground = theme.textPrimary
            background = theme.surface
            layout = GridLayout(1, false)
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            font = theme.bodyFont
        }

        Button(rightGroup, SWT.PUSH).apply {
            text = "🔄 Refresh Cookies"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    loadCookies()
                }
            })
        }

        cookieTable = Table(rightGroup, SWT.BORDER or SWT.FULL_SELECTION or SWT.V_SCROLL or SWT.H_SCROLL).apply {
            headerVisible = true
            linesVisible = true
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            font = theme.codeFont
        }

        val colName = TableColumn(cookieTable, SWT.LEFT).apply {
            text = "Cookie Name"
            width = 160
        }
        val colVal = TableColumn(cookieTable, SWT.LEFT).apply {
            text = "Value"
            width = 300
        }
    }

    private fun captureScreenshot() {
        val b = browser ?: run {
            statusLabel.text = "Waiting for browser..."
            return
        }
        statusLabel.text = "Capturing screenshot..."
        statusLabel.foreground = theme.primary
        b.view.takeScreenshotAsync().thenAccept { image ->
            display.asyncExec {
                if (!isDisposed) {
                    if (image != null) {
                        try {
                            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            val tempDir = System.getProperty("java.io.tmpdir")
                            val file = File(tempDir, "kromium_screenshot_$timestamp.png")
                            ImageIO.write(image, "PNG", file)
                            statusLabel.text = "Saved: ${file.name} (${image.width}x${image.height})"
                            statusLabel.foreground = theme.success
                        } catch (ex: Throwable) {
                            statusLabel.text = "Error saving screenshot: ${ex.message}"
                            statusLabel.foreground = theme.error
                        }
                    } else {
                        statusLabel.text = "Screenshot returned null surface"
                        statusLabel.foreground = theme.error
                    }
                }
            }
        }
    }

    private fun exportPdf() {
        val b = browser ?: run {
            statusLabel.text = "Waiting for browser..."
            return
        }
        statusLabel.text = "Generating PDF..."
        statusLabel.foreground = theme.primary
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val tempDir = System.getProperty("java.io.tmpdir")
        val file = File(tempDir, "kromium_page_$timestamp.pdf")
        b.view.printToPdf(file.absolutePath).thenAccept { success ->
            display.asyncExec {
                if (!isDisposed) {
                    if (success) {
                        statusLabel.text = "Saved: ${file.name}"
                        statusLabel.foreground = theme.success
                    } else {
                        statusLabel.text = "Failed to export PDF"
                        statusLabel.foreground = theme.error
                    }
                }
            }
        }
    }

    private fun loadCookies() {
        val b = browser ?: return
        b.storage.getCookiesAsync().thenAccept { cookies ->
            display.asyncExec {
                if (!isDisposed && !cookieTable.isDisposed) {
                    cookieTable.setRedraw(false)
                    cookieTable.removeAll()
                    cookies.forEach { (name, value) ->
                        val item = TableItem(cookieTable, SWT.NONE)
                        item.setText(0, name)
                        item.setText(1, value)
                    }
                    cookieTable.setRedraw(true)
                }
            }
        }
    }
}
