package org.daviante.kromium.sample.javafx.ui

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.javafx.KromiumJavaFxCanvas
import org.daviante.kromium.sample.javafx.features.*
import org.daviante.kromium.sample.javafx.model.ConsoleMessage
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch

class JavaFxAppShell(
    private val browser: KromiumBrowser,
    private val client: KromiumClient,
    private val consoleLogs: ObservableList<ConsoleMessage>,
    private val onClearConsole: () -> Unit
) : BorderPane() {

    private val shellScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)

    // Top Navigation Controls
    private val brandBadge = Label("KROMIUM").apply { styleClass.add("kromium-badge") }
    private val backBtn = Button("◀").apply { isDisable = true }
    private val fwdBtn = Button("▶").apply { isDisable = true }
    private val reloadBtn = Button("⟳")
    private val stopBtn = Button("✖").apply {
        styleClass.add("button-primary")
        isVisible = false
        isManaged = false
    }
    private val addressBar = TextField("https://example.com").apply {
        styleClass.add("address-bar")
        promptText = "Enter URL (e.g. example.com, github.com)..."
        HBox.setHgrow(this, Priority.ALWAYS)
    }
    private val goBtn = Button("Go").apply { styleClass.add("button-primary") }

    // Quick presets
    private val presets = listOf(
        "Google" to "https://www.google.com",
        "GitHub" to "https://github.com/daviante-org/kromium",
        "Wikipedia" to "https://www.wikipedia.org",
        "WebGL Aquarium" to "https://webglsamples.org/aquarium/aquarium.html",
        "Acid3 Compliance" to "http://acid3.acidtests.org/"
    )

    // Action Controls
    private val zoomOutBtn = Button("−")
    private val zoomResetBtn = Button("100%")
    private val zoomInBtn = Button("+")
    private val devToolsBtn = Button("🛠 DevTools").apply { styleClass.add("button-accent") }
    private val toggleToolsBtn = Button("⊟ Workbench")

    // Containers
    private val browserCanvas = (browser.view.surface.unwrap(KromiumJavaFxCanvas::class) ?: KromiumJavaFxCanvas()).apply {
        attach(browser)
    }
    private val browserContainer = StackPane().apply {
        alignment = Pos.TOP_LEFT
        isSnapToPixel = true
    }
    private val workbenchTabPane = TabPane()
    private val splitPane = SplitPane().apply {
        isSnapToPixel = true
    }

    // Bottom Status Bar Controls
    private val statusLabel = Label("Ready").apply { styleClass.add("status-label") }
    private val loadingSpinner = ProgressIndicator().apply {
        prefWidth = 14.0
        prefHeight = 14.0
        isVisible = false
    }
    private val consoleBadge = Label("0 logs").apply { styleClass.add("status-badge") }
    private val engineBadge = Label("JCEF OSR • Blink 122").apply { styleClass.add("status-badge") }

    private var isWorkbenchVisible = true

    init {
        setupTopBar()
        setupCenterSplit()
        setupStatusBar()
        setupListeners()
        setupNavigationStateBinding()
    }

    private fun setupTopBar() {
        val navControls = HBox(6.0, backBtn, fwdBtn, reloadBtn, stopBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        val chipsBox = HBox(4.0).apply {
            alignment = Pos.CENTER_LEFT
            presets.forEach { (name, url) ->
                val chip = Button(name).apply {
                    styleClass.add("chip-button")
                    setOnAction {
                        addressBar.text = url
                        browser.navigation.loadUrl(url)
                        browserCanvas.requestFocus()
                    }
                }
                children.add(chip)
            }
        }

        val zoomBox = HBox(2.0, zoomOutBtn, zoomResetBtn, zoomInBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        val rightActions = HBox(6.0, zoomBox, devToolsBtn, toggleToolsBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        val topHeader = HBox(8.0, brandBadge, navControls, addressBar, goBtn, chipsBox, Separator(Orientation.VERTICAL), rightActions).apply {
            styleClass.add("kromium-header")
            alignment = Pos.CENTER_LEFT
        }

        top = topHeader
    }

    private fun setupCenterSplit() {
        browserContainer.style = "-fx-background-color: #0e1117;"
        browserContainer.children.add(browserCanvas)

        // Setup Workbench Tabs
        workbenchTabPane.minWidth = 380.0
        workbenchTabPane.prefWidth = 420.0
        workbenchTabPane.tabs.addAll(
            NavigationFeatureTab(browser),
            DomExtractionFeatureTab(browser),
            AutomationFeatureTab(browser, shellScope),
            DevToolsConsoleFeatureTab(browser, consoleLogs, onClearConsole),
            ExportFeatureTab(browser)
        )

        splitPane.items.addAll(browserContainer, workbenchTabPane)
        splitPane.setDividerPositions(0.70)

        center = splitPane
    }

    private fun setupStatusBar() {
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        val statusBar = HBox(10.0, statusLabel, loadingSpinner, spacer, consoleBadge, engineBadge).apply {
            styleClass.add("status-bar")
            alignment = Pos.CENTER_LEFT
        }
        bottom = statusBar

        consoleLogs.addListener(javafx.collections.ListChangeListener {
            consoleBadge.text = "${consoleLogs.size} logs"
        })
    }

    private fun setupListeners() {
        // Navigation actions
        backBtn.setOnAction { browser.navigation.goBack() }
        fwdBtn.setOnAction { browser.navigation.goForward() }
        reloadBtn.setOnAction { browser.navigation.reload() }
        stopBtn.setOnAction { browser.navigation.stopLoad() }

        val navigateToAddress = {
            var url = addressBar.text.trim()
            if (url.isNotBlank()) {
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("data:")) {
                    url = "https://$url"
                }
                browser.navigation.loadUrl(url)
                browserCanvas.requestFocus()
            }
        }

        goBtn.setOnAction { navigateToAddress() }
        addressBar.setOnKeyPressed { e ->
            if (e.code == KeyCode.ENTER) {
                navigateToAddress()
            }
        }

        // Zoom controls
        zoomInBtn.setOnAction {
            val nextZoom = (browser.view.zoomLevel + 0.5).coerceAtMost(4.0)
            browser.view.setZoom(nextZoom)
            updateZoomBadge()
        }
        zoomOutBtn.setOnAction {
            val nextZoom = (browser.view.zoomLevel - 0.5).coerceAtLeast(-2.0)
            browser.view.setZoom(nextZoom)
            updateZoomBadge()
        }
        zoomResetBtn.setOnAction {
            browser.view.setZoom(0.0)
            updateZoomBadge()
        }

        // DevTools
        devToolsBtn.setOnAction {
            browser.devTools.openDevTools()
        }

        // Toggle Workbench
        toggleToolsBtn.setOnAction {
            isWorkbenchVisible = !isWorkbenchVisible
            if (isWorkbenchVisible) {
                if (!splitPane.items.contains(workbenchTabPane)) {
                    splitPane.items.add(workbenchTabPane)
                    splitPane.setDividerPositions(0.70)
                }
                toggleToolsBtn.text = "Hide Tools ⇥"
            } else {
                splitPane.items.remove(workbenchTabPane)
                toggleToolsBtn.text = "Show Tools ⇤"
            }
        }
    }

    private fun updateZoomBadge() {
        val pct = ((browser.view.zoomLevel + 1.0) * 100).toInt()
        zoomResetBtn.text = "$pct%"
    }

    private fun setupNavigationStateBinding() {
        shellScope.launch {
            browser.navigation.navigationState.collectLatest { state ->
                // Keep address bar in sync unless the user is actively editing it
                if (!addressBar.isFocused && state.url.isNotBlank()) {
                    addressBar.text = state.url
                }

                backBtn.isDisable = !state.canGoBack
                fwdBtn.isDisable = !state.canGoForward

                if (state.isLoading) {
                    statusLabel.text = "Loading: ${state.url}"
                    reloadBtn.isVisible = false
                    reloadBtn.isManaged = false
                    stopBtn.isVisible = true
                    stopBtn.isManaged = true
                    loadingSpinner.isVisible = true
                } else {
                    statusLabel.text = if (state.url.isNotBlank()) "Loaded: ${state.url}" else "Ready"
                    reloadBtn.isVisible = true
                    reloadBtn.isManaged = true
                    stopBtn.isVisible = false
                    stopBtn.isManaged = false
                    loadingSpinner.isVisible = false
                }
            }
        }
    }
}
