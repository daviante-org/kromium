package org.daviante.kromium.sample.awt.ui

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.sample.awt.model.ConsoleEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JScrollPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AwtAppShell(
    private val frame: Frame,
    private val onClearConsole: () -> Unit
) : AwtPanel(BorderLayout()) {

    private var browser: KromiumBrowser? = null
    private var client: KromiumClient? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // UI Controls (Lightweight cross-platform AWT components)
    private val backBtn = AwtButton("◀")
    private val fwdBtn = AwtButton("▶")
    private val reloadBtn = AwtButton("⟳")
    private val stopBtn = AwtButton("✕")
    private val addressBar = AwtTextField("https://example.com", 36)
    private val goBtn = AwtButton("Go", isPrimary = true)
    private val zoomOutBtn = AwtButton("−")
    private val zoomResetBtn = AwtButton("100%")
    private val zoomInBtn = AwtButton("+")
    private val devToolsBtn = AwtButton("DevTools", isPrimary = true)
    private val toggleWorkbenchBtn = AwtButton("Workbench")

    // Containers
    private val browserHolder = AwtPanel(BorderLayout())
    private val workbenchPanel = AwtPanel(BorderLayout())
    private val cardLayout = CardLayout()
    private val tabContentPanel = AwtPanel(cardLayout)

    // Console Tab
    private val allLogs = CopyOnWriteArrayList<ConsoleEntry>()
    private val consoleFilter = AwtTextField("", 18)
    private val consoleTextArea = AwtTextArea("", 8, 80)
    private val consoleCountLabel = AwtLabel("0 logs", customColor = AwtTheme.textSecondary)

    // Automation Tab
    private val jsInput = AwtTextArea("document.title", 3, 60)
    private val jsOutput = AwtTextArea("", 6, 60)
    private val jsStatusLabel = AwtLabel("Ready", customColor = AwtTheme.textSecondary)

    // Tools Tab
    private val toolsStatusLabel = AwtLabel("Ready", customColor = AwtTheme.textSecondary)

    // Status Bar
    private val statusLabel = AwtLabel("Bootstrapping Chromium OSR Engine...", customColor = AwtTheme.primary)
    private val zoomLabel = AwtLabel("Zoom: 100%", customColor = AwtTheme.textSecondary)
    private val infoLabel = AwtLabel("Kromium | JCEF OSR | AWT", customColor = AwtTheme.textSecondary)

    private var isWorkbenchVisible = true

    init {
        background = AwtTheme.background

        setupTopBar()
        setupCenterArea()
        setupStatusBar()
    }

    private fun setupTopBar() {
        val topContainer = AwtPanel(BorderLayout())
        topContainer.background = AwtTheme.surface

        // Row 1: Main Controls
        val navBar = AwtPanel(FlowLayout(FlowLayout.LEFT, 6, 6))
        navBar.background = AwtTheme.surface

        val brandLabel = AwtLabel("KROMIUM", isBold = true, customColor = AwtTheme.primary)
        navBar.add(brandLabel)

        backBtn.isEnabled = false
        backBtn.addActionListener { browser?.navigation?.goBack() }
        navBar.add(backBtn)

        fwdBtn.isEnabled = false
        fwdBtn.addActionListener { browser?.navigation?.goForward() }
        navBar.add(fwdBtn)

        reloadBtn.addActionListener { browser?.navigation?.reload() }
        navBar.add(reloadBtn)

        stopBtn.isEnabled = false
        stopBtn.addActionListener { browser?.navigation?.stopLoad() }
        navBar.add(stopBtn)

        addressBar.addActionListener { navigateToCurrentUrl() }
        navBar.add(addressBar)

        goBtn.addActionListener { navigateToCurrentUrl() }
        navBar.add(goBtn)

        zoomOutBtn.addActionListener {
            val b = browser ?: return@addActionListener
            b.view.zoomLevel -= 0.5
            updateZoomDisplay()
        }
        navBar.add(zoomOutBtn)

        zoomResetBtn.addActionListener {
            val b = browser ?: return@addActionListener
            b.view.zoomLevel = 0.0
            updateZoomDisplay()
        }
        navBar.add(zoomResetBtn)

        zoomInBtn.addActionListener {
            val b = browser ?: return@addActionListener
            b.view.zoomLevel += 0.5
            updateZoomDisplay()
        }
        navBar.add(zoomInBtn)

        devToolsBtn.addActionListener { browser?.devTools?.openDevTools() }
        navBar.add(devToolsBtn)

        toggleWorkbenchBtn.addActionListener { toggleWorkbench() }
        navBar.add(toggleWorkbenchBtn)

        topContainer.add(navBar, BorderLayout.NORTH)

        // Row 2: Quick Bookmarks
        val bookmarksBar = AwtPanel(FlowLayout(FlowLayout.LEFT, 6, 4))
        bookmarksBar.background = AwtTheme.surfaceVariant

        val quickSitesLabel = AwtLabel("Quick Sites:", customColor = AwtTheme.textSecondary)
        bookmarksBar.add(quickSitesLabel)

        fun addBookmark(title: String, url: String) {
            val btn = AwtButton(title).apply {
                font = AwtTheme.smallFont
                addActionListener {
                    addressBar.text = url
                    browser?.navigation?.loadUrl(url)
                }
            }
            bookmarksBar.add(btn)
        }

        addBookmark("Google", "https://www.google.com")
        addBookmark("GitHub", "https://github.com/daviante-org/kromium")
        addBookmark("Wikipedia", "https://www.wikipedia.org")
        addBookmark("WebGL Aquarium", "https://webglsamples.org/aquarium/aquarium.html")
        addBookmark("Acid3 Compliance", "http://acid3.acidtests.org/")

        topContainer.add(bookmarksBar, BorderLayout.SOUTH)
        add(topContainer, BorderLayout.NORTH)
    }

    private fun setupCenterArea() {
        val centerPanel = AwtPanel(BorderLayout())
        centerPanel.background = AwtTheme.background

        // Browser Viewport Placeholder
        browserHolder.background = AwtTheme.background
        val splashPanel = AwtPanel(GridBagLayout())
        splashPanel.background = AwtTheme.background
        val splashLabel = AwtLabel("KROMIUM — Bootstrapping Chromium OSR Engine...", isBold = true, customColor = AwtTheme.primary)
        splashPanel.add(splashLabel)
        browserHolder.add(splashPanel, BorderLayout.CENTER)

        centerPanel.add(browserHolder, BorderLayout.CENTER)

        // Workbench Drawer
        setupWorkbench()
        centerPanel.add(workbenchPanel, BorderLayout.SOUTH)

        add(centerPanel, BorderLayout.CENTER)
    }

    private fun setupWorkbench() {
        workbenchPanel.background = AwtTheme.surface
        workbenchPanel.preferredSize = Dimension(1200, 220)

        // Tab Selector Row
        val tabHeader = AwtPanel(FlowLayout(FlowLayout.LEFT, 6, 4))
        tabHeader.background = AwtTheme.surfaceVariant

        val consoleTabBtn = AwtButton("Console Logs").apply {
            addActionListener { cardLayout.show(tabContentPanel, "CONSOLE") }
        }
        tabHeader.add(consoleTabBtn)

        val autoTabBtn = AwtButton("JS & Automation").apply {
            addActionListener { cardLayout.show(tabContentPanel, "AUTOMATION") }
        }
        tabHeader.add(autoTabBtn)

        val toolsTabBtn = AwtButton("Tools & Storage").apply {
            addActionListener { cardLayout.show(tabContentPanel, "TOOLS") }
        }
        tabHeader.add(toolsTabBtn)

        workbenchPanel.add(tabHeader, BorderLayout.NORTH)

        // Tab Cards
        tabContentPanel.background = AwtTheme.surface
        tabContentPanel.add(createConsoleTab(), "CONSOLE")
        tabContentPanel.add(createAutomationTab(), "AUTOMATION")
        tabContentPanel.add(createToolsTab(), "TOOLS")

        workbenchPanel.add(tabContentPanel, BorderLayout.CENTER)
    }

    private fun createConsoleTab(): AwtPanel {
        val panel = AwtPanel(BorderLayout())
        panel.background = AwtTheme.surface

        val topBar = AwtPanel(FlowLayout(FlowLayout.LEFT, 6, 4))
        topBar.background = AwtTheme.surface

        val filterLabel = AwtLabel("Filter:", customColor = AwtTheme.textSecondary)
        topBar.add(filterLabel)

        consoleFilter.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refreshConsoleOutput() }
            override fun removeUpdate(e: DocumentEvent?) { refreshConsoleOutput() }
            override fun changedUpdate(e: DocumentEvent?) { refreshConsoleOutput() }
        })
        topBar.add(consoleFilter)

        val clearBtn = AwtButton("Clear").apply {
            font = AwtTheme.smallFont
            addActionListener {
                allLogs.clear()
                onClearConsole()
                refreshConsoleOutput()
            }
        }
        topBar.add(clearBtn)

        topBar.add(consoleCountLabel)
        panel.add(topBar, BorderLayout.NORTH)

        consoleTextArea.isEditable = false
        val scrollPane = JScrollPane(consoleTextArea).apply {
            border = BorderFactory.createLineBorder(AwtTheme.border, 1)
            viewport.background = AwtTheme.surface
        }
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createAutomationTab(): AwtPanel {
        val panel = AwtPanel(BorderLayout())
        panel.background = AwtTheme.surface

        // Snippets bar
        val snippetsBar = AwtPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        snippetsBar.background = AwtTheme.surface

        fun addSnippet(name: String, code: String) {
            val btn = AwtButton(name).apply {
                font = AwtTheme.smallFont
                addActionListener {
                    jsInput.text = code
                    runScript(code)
                }
            }
            snippetsBar.add(btn)
        }

        addSnippet("Title", "document.title")
        addSnippet("User-Agent", "navigator.userAgent")
        addSnippet("Extract H1s", "Array.from(document.querySelectorAll('h1')).map(e => e.innerText).join(', ')")
        addSnippet("Links Count", "document.querySelectorAll('a').length")
        addSnippet("Toggle Invert", "document.body.style.filter = document.body.style.filter ? '' : 'invert(1)'")

        panel.add(snippetsBar, BorderLayout.NORTH)

        val centerSplit = AwtPanel(GridLayout(2, 1, 4, 4))
        centerSplit.background = AwtTheme.surface

        // Input Editor
        val inputHolder = AwtPanel(BorderLayout())
        inputHolder.background = AwtTheme.surface
        val inputScroll = JScrollPane(jsInput).apply {
            border = BorderFactory.createLineBorder(AwtTheme.border, 1)
            viewport.background = AwtTheme.surfaceVariant
        }
        inputHolder.add(inputScroll, BorderLayout.CENTER)

        val actionRow = AwtPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        actionRow.background = AwtTheme.surface

        val evalBtn = AwtButton("Evaluate Expression", isPrimary = true).apply {
            font = AwtTheme.smallFont
            addActionListener { runScript(jsInput.text.trim()) }
        }
        actionRow.add(evalBtn)

        val htmlBtn = AwtButton("Fetch Full HTML").apply {
            font = AwtTheme.smallFont
            addActionListener { fetchHtml() }
        }
        actionRow.add(htmlBtn)

        val textBtn = AwtButton("Fetch Visible Text").apply {
            font = AwtTheme.smallFont
            addActionListener { fetchVisibleText() }
        }
        actionRow.add(textBtn)

        actionRow.add(jsStatusLabel)

        inputHolder.add(actionRow, BorderLayout.SOUTH)
        centerSplit.add(inputHolder)

        // Output Display
        jsOutput.isEditable = false
        val outputScroll = JScrollPane(jsOutput).apply {
            border = BorderFactory.createLineBorder(AwtTheme.border, 1)
            viewport.background = AwtTheme.surface
        }
        centerSplit.add(outputScroll)

        panel.add(centerSplit, BorderLayout.CENTER)

        return panel
    }

    private fun createToolsTab(): AwtPanel {
        val panel = AwtPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
        panel.background = AwtTheme.surface

        val screenshotBtn = AwtButton("Capture Screenshot").apply {
            addActionListener { captureScreenshot() }
        }
        panel.add(screenshotBtn)

        val pdfBtn = AwtButton("Export to PDF").apply {
            addActionListener { exportPdf() }
        }
        panel.add(pdfBtn)

        val clearCookiesBtn = AwtButton("Clear Cookies").apply {
            addActionListener {
                val b = browser ?: return@addActionListener
                val ok = b.storage.clearCookies()
                toolsStatusLabel.text = if (ok) "Cookies cleared successfully" else "Failed to clear cookies"
                toolsStatusLabel.foreground = if (ok) AwtTheme.success else AwtTheme.error
            }
        }
        panel.add(clearCookiesBtn)

        val clearStorageBtn = AwtButton("Clear Web Storage").apply {
            addActionListener {
                val b = browser ?: return@addActionListener
                b.storage.clearWebStorageAsync().thenAccept { ok ->
                    EventQueue.invokeLater {
                        toolsStatusLabel.text = if (ok) "Storage cleared successfully" else "Failed to clear storage"
                        toolsStatusLabel.foreground = if (ok) AwtTheme.success else AwtTheme.error
                    }
                }
            }
        }
        panel.add(clearStorageBtn)

        panel.add(toolsStatusLabel)

        return panel
    }

    private fun setupStatusBar() {
        val statusBar = AwtPanel(BorderLayout(10, 0))
        statusBar.background = AwtTheme.surface

        statusBar.add(statusLabel, BorderLayout.WEST)
        statusBar.add(zoomLabel, BorderLayout.CENTER)
        statusBar.add(infoLabel, BorderLayout.EAST)

        add(statusBar, BorderLayout.SOUTH)
    }

    fun attachBrowser(createdBrowser: KromiumBrowser, createdClient: KromiumClient) {
        this.browser = createdBrowser
        this.client = createdClient

        val uiComp = createdBrowser.view.surface.unwrap(Component::class)
        if (uiComp != null) {
            EventQueue.invokeLater {
                browserHolder.removeAll()
                browserHolder.add(uiComp, BorderLayout.CENTER)
                browserHolder.validate()
                browserHolder.repaint()
            }
        }

        statusLabel.text = "● Ready"
        statusLabel.foreground = AwtTheme.success

        observeNavigationState(createdBrowser)
    }

    fun onConsoleMessageAdded(entry: ConsoleEntry) {
        allLogs.add(0, entry)
        if (allLogs.size > 1000) {
            allLogs.removeAt(allLogs.size - 1)
        }
        refreshConsoleOutput()
    }

    private fun refreshConsoleOutput() {
        val filter = consoleFilter.text.trim().lowercase()
        val filtered = if (filter.isEmpty()) {
            allLogs
        } else {
            allLogs.filter {
                it.message.lowercase().contains(filter) ||
                it.source.lowercase().contains(filter) ||
                it.level.lowercase().contains(filter)
            }
        }

        val sb = StringBuilder()
        for (log in filtered) {
            val src = if (log.line > 0) "${log.source}:${log.line}" else log.source
            sb.append("[${log.timestamp}] [${log.level}] $src: ${log.message}\n")
        }
        consoleTextArea.text = sb.toString()
        consoleCountLabel.text = "${filtered.size} of ${allLogs.size} logs"
    }

    private fun navigateToCurrentUrl() {
        val b = browser ?: return
        var url = addressBar.text.trim()
        if (url.isNotEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("about:")) {
                url = "https://$url"
            }
            b.navigation.loadUrl(url)
        }
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
        workbenchPanel.isVisible = isWorkbenchVisible
        toggleWorkbenchBtn.text = if (isWorkbenchVisible) "Hide Workbench" else "Show Workbench"
        validate()
        repaint()
    }

    private fun runScript(code: String) {
        if (code.isBlank()) return
        val b = browser ?: run {
            jsStatusLabel.text = "Waiting for browser..."
            return
        }
        jsStatusLabel.text = "Evaluating..."
        jsStatusLabel.foreground = AwtTheme.primary
        scope.launch {
            try {
                val res = b.jsBridge.evaluateJavaScript(code)
                EventQueue.invokeLater {
                    jsOutput.text = res ?: "(null or undefined response)"
                    jsStatusLabel.text = "Success"
                    jsStatusLabel.foreground = AwtTheme.success
                }
            } catch (t: Throwable) {
                EventQueue.invokeLater {
                    jsOutput.text = "Evaluation Error: ${t.message}"
                    jsStatusLabel.text = "Failed"
                    jsStatusLabel.foreground = AwtTheme.error
                }
            }
        }
    }

    private fun fetchHtml() {
        val b = browser ?: return
        jsStatusLabel.text = "Fetching HTML..."
        jsStatusLabel.foreground = AwtTheme.primary
        scope.launch {
            try {
                val html = b.jsBridge.getHtml()
                EventQueue.invokeLater {
                    jsOutput.text = html
                    jsStatusLabel.text = "Fetched ${html.length} chars"
                    jsStatusLabel.foreground = AwtTheme.success
                }
            } catch (t: Throwable) {
                EventQueue.invokeLater {
                    jsOutput.text = "Error: ${t.message}"
                    jsStatusLabel.text = "Failed"
                    jsStatusLabel.foreground = AwtTheme.error
                }
            }
        }
    }

    private fun fetchVisibleText() {
        val b = browser ?: return
        jsStatusLabel.text = "Fetching Visible Text..."
        jsStatusLabel.foreground = AwtTheme.primary
        scope.launch {
            try {
                val text = b.jsBridge.evaluateJavaScript("document.body.innerText") ?: ""
                EventQueue.invokeLater {
                    jsOutput.text = text
                    jsStatusLabel.text = "Fetched ${text.length} chars"
                    jsStatusLabel.foreground = AwtTheme.success
                }
            } catch (t: Throwable) {
                EventQueue.invokeLater {
                    jsOutput.text = "Error: ${t.message}"
                    jsStatusLabel.text = "Failed"
                    jsStatusLabel.foreground = AwtTheme.error
                }
            }
        }
    }

    private fun captureScreenshot() {
        val b = browser ?: return
        toolsStatusLabel.text = "Capturing screenshot..."
        toolsStatusLabel.foreground = AwtTheme.primary
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(System.getProperty("user.home"), "Downloads/kromium_screenshot_$ts.png")
        file.parentFile?.mkdirs()

        b.view.takeScreenshotAsync().thenAccept { img ->
            EventQueue.invokeLater {
                if (img != null) {
                    try {
                        ImageIO.write(img, "PNG", file)
                        toolsStatusLabel.text = "Saved: ${file.name}"
                        toolsStatusLabel.foreground = AwtTheme.success
                    } catch (e: Exception) {
                        toolsStatusLabel.text = "Save error: ${e.message}"
                        toolsStatusLabel.foreground = AwtTheme.error
                    }
                } else {
                    toolsStatusLabel.text = "Screenshot returned null"
                    toolsStatusLabel.foreground = AwtTheme.error
                }
            }
        }.exceptionally { t ->
            EventQueue.invokeLater {
                toolsStatusLabel.text = "Screenshot failed: ${t.message}"
                toolsStatusLabel.foreground = AwtTheme.error
            }
            null
        }
    }

    private fun exportPdf() {
        val b = browser ?: return
        toolsStatusLabel.text = "Exporting PDF..."
        toolsStatusLabel.foreground = AwtTheme.primary
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(System.getProperty("user.home"), "Downloads/kromium_export_$ts.pdf")
        file.parentFile?.mkdirs()

        b.view.printToPdf(file.absolutePath).thenAccept { success ->
            EventQueue.invokeLater {
                if (success) {
                    toolsStatusLabel.text = "Exported: ${file.name}"
                    toolsStatusLabel.foreground = AwtTheme.success
                } else {
                    toolsStatusLabel.text = "PDF Export Failed"
                    toolsStatusLabel.foreground = AwtTheme.error
                }
            }
        }.exceptionally { t ->
            EventQueue.invokeLater {
                toolsStatusLabel.text = "PDF Export Error: ${t.message}"
                toolsStatusLabel.foreground = AwtTheme.error
            }
            null
        }
    }

    private fun observeNavigationState(browser: KromiumBrowser) {
        scope.launch {
            browser.navigation.navigationState.collectLatest { state ->
                EventQueue.invokeLater {
                    backBtn.isEnabled = state.canGoBack
                    fwdBtn.isEnabled = state.canGoForward
                    stopBtn.isEnabled = state.isLoading

                    if (state.url.isNotBlank() && state.url != "about:blank") {
                        addressBar.text = state.url
                    }

                    if (state.isLoading) {
                        statusLabel.text = "Loading ${state.url}..."
                        statusLabel.foreground = AwtTheme.primary
                    } else {
                        statusLabel.text = "● Ready"
                        statusLabel.foreground = AwtTheme.success
                    }

                    frame.title = if (state.url.isNotBlank() && state.url != "about:blank") {
                        "Kromium \u2014 ${state.url}"
                    } else {
                        "Kromium \u2014 AWT Desktop Browser"
                    }
                }
            }
        }
    }
}
