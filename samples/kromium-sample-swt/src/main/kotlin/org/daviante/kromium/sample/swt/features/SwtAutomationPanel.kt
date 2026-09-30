package org.daviante.kromium.sample.swt.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swt.ui.SwtTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class SwtAutomationPanel(
    parent: Composite,
    private var browser: KromiumBrowser?,
    private val theme: SwtTheme
) : Composite(parent, SWT.NONE) {

    private val inputEditor: Text
    private val outputText: Text
    private val statusLabel: Label
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setBrowser(b: KromiumBrowser) {
        this.browser = b
    }

    init {
        layout = GridLayout(1, false)
        layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        background = theme.surface

        // Top Snippet Buttons
        val snippetsBar = Composite(this, SWT.NONE).apply {
            layout = GridLayout(6, false)
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surface
        }

        fun addSnippetBtn(label: String, code: String) {
            Button(snippetsBar, SWT.PUSH).apply {
                text = label
                font = theme.smallFont
                addSelectionListener(object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent?) {
                        inputEditor.text = code
                        executeScript(code)
                    }
                })
            }
        }

        addSnippetBtn("Title", "document.title")
        addSnippetBtn("User-Agent", "navigator.userAgent")
        addSnippetBtn("Extract H1s", "Array.from(document.querySelectorAll('h1')).map(e => e.innerText).join(', ')")
        addSnippetBtn("All Links Count", "document.querySelectorAll('a').length")
        addSnippetBtn("Toggle Invert", "document.body.style.filter = document.body.style.filter ? '' : 'invert(1)'")
        addSnippetBtn("Window Dimensions", "window.innerWidth + 'x' + window.innerHeight")

        // Input Editor
        Label(this, SWT.NONE).apply {
            text = "JavaScript Expression / Code:"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
        }

        inputEditor = Text(this, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL).apply {
            text = "document.title"
            font = theme.codeFont
            val gd = GridData(SWT.FILL, SWT.CENTER, true, false)
            gd.heightHint = 65
            layoutData = gd
        }

        // Action Row
        val actionRow = Composite(this, SWT.NONE).apply {
            layout = GridLayout(4, false)
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surface
        }

        Button(actionRow, SWT.PUSH).apply {
            text = "▶ Evaluate Expression"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    executeScript(inputEditor.text.trim())
                }
            })
        }

        Button(actionRow, SWT.PUSH).apply {
            text = "Fetch Full HTML"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    fetchHtml()
                }
            })
        }

        Button(actionRow, SWT.PUSH).apply {
            text = "Fetch Visible Text"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    fetchVisibleText()
                }
            })
        }

        statusLabel = Label(actionRow, SWT.NONE).apply {
            text = "Ready"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        }

        // Output Display
        Label(this, SWT.NONE).apply {
            text = "Evaluation Output:"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
        }

        outputText = Text(this, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL or SWT.H_SCROLL or SWT.READ_ONLY).apply {
            font = theme.codeFont
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        }
    }

    private fun executeScript(code: String) {
        if (code.isBlank()) return
        val b = browser ?: run {
            statusLabel.text = "Waiting for browser..."
            return
        }
        statusLabel.text = "Evaluating..."
        statusLabel.foreground = theme.primary
        scope.launch {
            try {
                val result = b.jsBridge.evaluateJavaScript(code)
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = result ?: "(null or undefined response)"
                        statusLabel.text = "Success"
                        statusLabel.foreground = theme.success
                    }
                }
            } catch (t: Throwable) {
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = "Evaluation Error: ${t.message}"
                        statusLabel.text = "Failed"
                        statusLabel.foreground = theme.error
                    }
                }
            }
        }
    }

    private fun fetchHtml() {
        val b = browser ?: run {
            statusLabel.text = "Waiting for browser..."
            return
        }
        statusLabel.text = "Fetching HTML..."
        statusLabel.foreground = theme.primary
        scope.launch {
            try {
                val html = b.jsBridge.getHtml()
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = html
                        statusLabel.text = "Fetched ${html.length} chars"
                        statusLabel.foreground = theme.success
                    }
                }
            } catch (t: Throwable) {
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = "Error: ${t.message}"
                        statusLabel.text = "Failed"
                        statusLabel.foreground = theme.error
                    }
                }
            }
        }
    }

    private fun fetchVisibleText() {
        val b = browser ?: run {
            statusLabel.text = "Waiting for browser..."
            return
        }
        statusLabel.text = "Fetching Visible Text..."
        statusLabel.foreground = theme.primary
        scope.launch {
            try {
                val text = b.jsBridge.getText()
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = text
                        statusLabel.text = "Fetched ${text.length} chars"
                        statusLabel.foreground = theme.success
                    }
                }
            } catch (t: Throwable) {
                display.asyncExec {
                    if (!isDisposed) {
                        outputText.text = "Error: ${t.message}"
                        statusLabel.text = "Failed"
                        statusLabel.foreground = theme.error
                    }
                }
            }
        }
    }
}
