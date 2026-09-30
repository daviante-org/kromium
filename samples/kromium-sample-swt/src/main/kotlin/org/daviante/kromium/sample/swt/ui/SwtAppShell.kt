package org.daviante.kromium.sample.swt.ui

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.swt.KromiumSwtCanvas
import org.daviante.kromium.sample.swt.features.SwtAutomationPanel
import org.daviante.kromium.sample.swt.features.SwtConsolePanel
import org.daviante.kromium.sample.swt.features.SwtToolsPanel
import org.daviante.kromium.sample.swt.model.ConsoleEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class SwtAppShell(
    private val shell: Shell,
    private val theme: SwtTheme,
    private val onClearConsole: () -> Unit
) : Composite(shell, SWT.NONE) {

    private var browser: KromiumBrowser? = null
    private var client: KromiumClient? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // UI Controls
    private val backBtn: Button
    private val fwdBtn: Button
    private val reloadBtn: Button
    private val stopBtn: Button
    private val addressBar: Text
    private val goBtn: Button
    private val zoomOutBtn: Button
    private val zoomResetBtn: Button
    private val zoomInBtn: Button
    private val devToolsBtn: Button
    private val toggleWorkbenchBtn: Button

    // Main Layout
    private val sashForm: SashForm
    private val browserCanvas: KromiumSwtCanvas
    private val loadingProgress: ProgressBar
    private val tabFolder: CTabFolder
    private val consolePanel: SwtConsolePanel
    private val autoPanel: SwtAutomationPanel
    private val toolsPanel: SwtToolsPanel

    // Status Bar
    private val statusLabel: Label
    private val zoomLabel: Label
    private val infoLabel: Label

    private var isWorkbenchVisible = true
    private var lastSavedWeights = intArrayOf(72, 28)

    init {
        layout = GridLayout(1, false).apply {
            marginWidth = 0
            marginHeight = 0
            verticalSpacing = 0
        }
        layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        background = theme.background

        // 1. Navigation Top Bar
        val topBar = Composite(this, SWT.NONE).apply {
            layout = GridLayout(12, false).apply {
                marginWidth = 8
                marginHeight = 6
                horizontalSpacing = 6
            }
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surface
        }

        // Brand Badge
        Label(topBar, SWT.NONE).apply {
            text = "KROMIUM"
            foreground = theme.primary
            background = theme.surface
            font = theme.titleFont
        }

        backBtn = Button(topBar, SWT.PUSH).apply {
            text = "◀"
            font = theme.smallFont
            isEnabled = false
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { browser?.navigation?.goBack() }
            })
        }

        fwdBtn = Button(topBar, SWT.PUSH).apply {
            text = "▶"
            font = theme.smallFont
            isEnabled = false
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { browser?.navigation?.goForward() }
            })
        }

        reloadBtn = Button(topBar, SWT.PUSH).apply {
            text = "⟳"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { browser?.navigation?.reload() }
            })
        }

        stopBtn = Button(topBar, SWT.PUSH).apply {
            text = "✖"
            font = theme.smallFont
            isEnabled = false
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { browser?.navigation?.stopLoad() }
            })
        }

        addressBar = Text(topBar, SWT.BORDER).apply {
            text = "https://example.com"
            font = theme.bodyFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetDefaultSelected(e: SelectionEvent?) { navigateToCurrentUrl() }
            })
        }

        goBtn = Button(topBar, SWT.PUSH).apply {
            text = "Go"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { navigateToCurrentUrl() }
            })
        }

        // Zoom Controls
        zoomOutBtn = Button(topBar, SWT.PUSH).apply {
            text = "−"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val b = browser ?: return
                    b.view.zoomLevel -= 0.5
                    updateZoomDisplay()
                }
            })
        }

        zoomResetBtn = Button(topBar, SWT.PUSH).apply {
            text = "100%"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val b = browser ?: return
                    b.view.zoomLevel = 0.0
                    updateZoomDisplay()
                }
            })
        }

        zoomInBtn = Button(topBar, SWT.PUSH).apply {
            text = "+"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val b = browser ?: return
                    b.view.zoomLevel += 0.5
                    updateZoomDisplay()
                }
            })
        }

        devToolsBtn = Button(topBar, SWT.PUSH).apply {
            text = "🛠 DevTools"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    browser?.devTools?.openDevTools()
                }
            })
        }

        toggleWorkbenchBtn = Button(topBar, SWT.PUSH).apply {
            text = "⊟ Workbench"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) { toggleWorkbench() }
            })
        }

        // 2. Quick Bookmarks Bar
        val bookmarksBar = Composite(this, SWT.NONE).apply {
            layout = GridLayout(7, false).apply {
                marginWidth = 8
                marginHeight = 4
                horizontalSpacing = 6
            }
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surfaceVariant
        }

        Label(bookmarksBar, SWT.NONE).apply {
            text = "Quick Sites:"
            foreground = theme.textSecondary
            background = theme.surfaceVariant
            font = theme.smallFont
        }

        fun addBookmark(title: String, url: String) {
            Button(bookmarksBar, SWT.PUSH).apply {
                text = title
                font = theme.smallFont
                addSelectionListener(object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent?) {
                        addressBar.text = url
                        browser?.navigation?.loadUrl(url)
                        browserCanvas.forceFocus()
                    }
                })
            }
        }

        addBookmark("Google", "https://www.google.com")
        addBookmark("GitHub", "https://github.com/daviante-org/kromium")
        addBookmark("Wikipedia", "https://www.wikipedia.org")
        addBookmark("WebGL Aquarium", "https://webglsamples.org/aquarium/aquarium.html")
        addBookmark("Acid3 Compliance", "http://acid3.acidtests.org/")

        // 3. Central SashForm with Browser View and Workbench
        sashForm = SashForm(this, SWT.VERTICAL).apply {
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            background = theme.border
        }

        // Browser View Container
        val browserContainer = Composite(sashForm, SWT.NONE).apply {
            layout = GridLayout(1, false).apply {
                marginWidth = 0
                marginHeight = 0
                verticalSpacing = 0
            }
            background = theme.background
        }

        loadingProgress = ProgressBar(browserContainer, SWT.SMOOTH or SWT.INDETERMINATE).apply {
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false).apply {
                heightHint = 3
            }
            isVisible = true
        }

        // Initialize native Kromium SWT Canvas
        browserCanvas = KromiumSwtCanvas(browserContainer).apply {
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        }

        // Workbench Drawer (Tabs)
        tabFolder = CTabFolder(sashForm, SWT.BORDER or SWT.FLAT).apply {
            background = theme.surface
            foreground = theme.textPrimary
            selectionBackground = theme.surfaceVariant
            selectionForeground = theme.primary
            font = theme.smallFont
        }

        // Tab 1: Console Logs
        val consoleTab = CTabItem(tabFolder, SWT.NONE).apply { text = "Console Logs" }
        consolePanel = SwtConsolePanel(tabFolder, theme, onClearConsole)
        consoleTab.control = consolePanel

        // Tab 2: Automation & JS
        val autoTab = CTabItem(tabFolder, SWT.NONE).apply { text = "JS & DOM Automation" }
        autoPanel = SwtAutomationPanel(tabFolder, null, theme)
        autoTab.control = autoPanel

        // Tab 3: Tools & Storage
        val toolsTab = CTabItem(tabFolder, SWT.NONE).apply { text = "Tools & Storage" }
        toolsPanel = SwtToolsPanel(tabFolder, null, theme)
        toolsTab.control = toolsPanel

        tabFolder.selection = consoleTab
        sashForm.setWeights(72, 28)

        // 4. Bottom Status Bar
        val statusBar = Composite(this, SWT.NONE).apply {
            layout = GridLayout(3, false).apply {
                marginWidth = 8
                marginHeight = 4
            }
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surface
        }

        statusLabel = Label(statusBar, SWT.NONE).apply {
            text = "⏳ Bootstrapping Chromium OSR Engine..."
            foreground = theme.primary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        }

        zoomLabel = Label(statusBar, SWT.NONE).apply {
            text = "Zoom: 100%"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.CENTER, SWT.CENTER, false, false)
        }

        infoLabel = Label(statusBar, SWT.NONE).apply {
            text = "Kromium | JCEF OSR | SWT 3.128.0"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.END, SWT.CENTER, false, false)
        }
    }

    fun attachBrowser(createdBrowser: KromiumBrowser, createdClient: KromiumClient) {
        this.browser = createdBrowser
        this.client = createdClient

        autoPanel.setBrowser(createdBrowser)
        toolsPanel.setBrowser(createdBrowser)

        browserCanvas.attach(createdBrowser)

        statusLabel.text = "● Ready"
        statusLabel.foreground = theme.success
        loadingProgress.isVisible = false

        observeNavigationState(createdBrowser)
    }

    fun onConsoleMessageAdded(entry: ConsoleEntry) {
        display.asyncExec {
            if (!isDisposed) {
                consolePanel.addLog(entry)
            }
        }
    }

    private fun navigateToCurrentUrl() {
        val b = browser ?: return
        val raw = addressBar.text.trim()
        if (raw.isEmpty()) return
        val target = if (!raw.startsWith("http://") && !raw.startsWith("https://") && !raw.startsWith("file://") && !raw.startsWith("about:")) {
            "https://$raw"
        } else raw
        b.navigation.loadUrl(target)
        browserCanvas.forceFocus()
    }

    private fun updateZoomDisplay() {
        val b = browser ?: return
        val zoom = b.view.zoomLevel
        val percentage = (100 * Math.pow(1.2, zoom)).toInt()
        zoomResetBtn.text = "$percentage%"
        zoomLabel.text = "Zoom: $percentage%"
    }

    private fun toggleWorkbench() {
        isWorkbenchVisible = !isWorkbenchVisible
        if (isWorkbenchVisible) {
            sashForm.setWeights(*lastSavedWeights)
            toggleWorkbenchBtn.text = "⊟ Workbench"
        } else {
            lastSavedWeights = sashForm.weights
            sashForm.setWeights(100, 0)
            toggleWorkbenchBtn.text = "⊞ Workbench"
        }
    }

    private fun observeNavigationState(b: KromiumBrowser) {
        scope.launch {
            b.navigation.navigationState.collectLatest { state ->
                display.asyncExec {
                    if (!isDisposed && !shell.isDisposed) {
                        backBtn.isEnabled = state.canGoBack
                        fwdBtn.isEnabled = state.canGoForward
                        stopBtn.isEnabled = state.isLoading
                        loadingProgress.isVisible = state.isLoading

                        if (!addressBar.isFocusControl && state.url.isNotBlank() && state.url != "about:blank") {
                            addressBar.text = state.url
                        }

                        if (state.isLoading) {
                            statusLabel.text = "⏳ Loading ${state.url}..."
                            statusLabel.foreground = theme.primary
                        } else {
                            statusLabel.text = "● Ready"
                            statusLabel.foreground = theme.success
                        }

                        shell.text = if (state.url.isNotBlank() && state.url != "about:blank") {
                            "Kromium \u2014 ${state.url}"
                        } else {
                            "Kromium \u2014 SWT Desktop Browser"
                        }
                    }
                }
            }
        }
    }
}
