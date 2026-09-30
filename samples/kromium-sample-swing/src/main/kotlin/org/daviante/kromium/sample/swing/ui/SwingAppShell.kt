package org.daviante.kromium.sample.swing.ui

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.sample.swing.features.AutomationFeaturePanel
import org.daviante.kromium.sample.swing.features.CookieStorageFeaturePanel
import org.daviante.kromium.sample.swing.features.DevToolsFeaturePanel
import org.daviante.kromium.sample.swing.features.DomExtractionFeaturePanel
import org.daviante.kromium.sample.swing.features.DownloadsFeaturePanel
import org.daviante.kromium.sample.swing.features.NavigationFeaturePanel
import org.daviante.kromium.sample.swing.features.PdfPrintFeaturePanel
import org.daviante.kromium.sample.swing.features.SecurityNetworkFeaturePanel
import org.daviante.kromium.sample.swing.features.ViewToolsFeaturePanel
import org.daviante.kromium.sample.swing.model.ConsoleMessage
import org.daviante.kromium.sample.swing.model.SampleFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EnumMap
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class SwingAppShell(
    private val browser: KromiumBrowser,
    private val client: KromiumClient,
    private val consoleLogs: MutableList<ConsoleMessage>,
    private val onClearConsole: () -> Unit
) : JPanel() {

    private val shellScope = CoroutineScope(Dispatchers.Swing + Job())

    private var selectedFeature = SampleFeature.NAVIGATION
    private var isWorkbenchVisible = true

    // Top bar components
    private val brandingLabel = KromiumLabel("KROMIUM", isBold = true, isMonospace = true, fontSize = 13f)
    private val backBtn = KromiumButton("◀", fontSize = 12f)
    private val forwardBtn = KromiumButton("▶", fontSize = 12f)
    private val reloadBtn = KromiumButton("⟳", fontSize = 14f)
    private val stopBtn = KromiumButton("✖", isPrimary = true, fontSize = 12f)
    private val addressBar = KromiumTextField(placeholder = "Enter URL (e.g. example.com, wikipedia.org)...", fontSize = 12f)
    private val goBtn = KromiumButton("Go", isPrimary = true, fontSize = 12f)
    private val toggleToolsBtn = KromiumButton("Hide Tools ⇥", fontSize = 11f)
    private val toggleThemeBtn = KromiumButton(if (SwingTheme.isDark) "Light" else "Dark", fontSize = 11f)

    // Layout containers
    private val mainSplitPanel = JPanel(BorderLayout())
    private val leftSidebarPanel = JPanel()
    private val leftSidebarHolder = JPanel(BorderLayout())
    private val rightWorkbenchPanel = JPanel(BorderLayout())
    private val centerBrowserHolder = JPanel(BorderLayout())

    // Cached feature panels
    private val featurePanels = EnumMap<SampleFeature, JPanel>(SampleFeature::class.java)
    private val featureSidebarButtons = EnumMap<SampleFeature, JPanel>(SampleFeature::class.java)

    init {
        layout = BorderLayout()
        isOpaque = true

        SwingTheme.addThemeListener {
            background = SwingTheme.background
            toggleThemeBtn.text = if (SwingTheme.isDark) "Light" else "Dark"
            leftSidebarPanel.background = SwingTheme.surface
            rightWorkbenchPanel.background = SwingTheme.surface
            revalidate()
            repaint()
        }
        background = SwingTheme.background

        initFeaturePanels()
        setupTopBar()
        setupMainSplit()
        setupListeners()
    }

    private fun initFeaturePanels() {
        featurePanels[SampleFeature.NAVIGATION] = NavigationFeaturePanel(browser)
        featurePanels[SampleFeature.DOM_EXTRACTION] = DomExtractionFeaturePanel(browser)
        featurePanels[SampleFeature.DOWNLOADS] = DownloadsFeaturePanel(browser, client)
        featurePanels[SampleFeature.VIEW_TOOLS] = ViewToolsFeaturePanel(browser)
        featurePanels[SampleFeature.AUTOMATION] = AutomationFeaturePanel(browser)
        featurePanels[SampleFeature.COOKIE_STORAGE] = CookieStorageFeaturePanel(browser)
        featurePanels[SampleFeature.PDF_PRINT] = PdfPrintFeaturePanel(browser)
        featurePanels[SampleFeature.DEVTOOLS] = DevToolsFeaturePanel(browser, consoleLogs, onClearConsole)
        featurePanels[SampleFeature.SECURITY_NETWORK] = SecurityNetworkFeaturePanel(browser, client)
    }

    private fun setupTopBar() {
        val topContainer = JPanel()
        topContainer.layout = BoxLayout(topContainer, BoxLayout.Y_AXIS)
        topContainer.isOpaque = false

        val bar = JPanel(BorderLayout(8, 0))
        bar.isOpaque = false
        bar.border = EmptyBorder(8, 12, 8, 12)

        // Left section of top bar: Traffic lights, branding, divider, back/forward/reload
        val leftControls = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        leftControls.isOpaque = false
        leftControls.add(TrafficLightsComponent())
        leftControls.add(Box.createHorizontalStrut(4))
        leftControls.add(brandingLabel)
        leftControls.add(VerticalDivider(22))

        backBtn.preferredSize = Dimension(38, 34)
        forwardBtn.preferredSize = Dimension(38, 34)
        reloadBtn.preferredSize = Dimension(38, 34)
        stopBtn.preferredSize = Dimension(38, 34)
        stopBtn.isVisible = false

        leftControls.add(backBtn)
        leftControls.add(forwardBtn)
        leftControls.add(reloadBtn)
        leftControls.add(stopBtn)

        bar.add(leftControls, BorderLayout.WEST)

        // Center: Address bar
        bar.add(addressBar, BorderLayout.CENTER)

        // Right section: Go, Divider, Presets, Divider, Toggle Tools, Toggle Theme
        val rightControls = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0))
        rightControls.isOpaque = false

        goBtn.preferredSize = Dimension(48, 34)
        rightControls.add(goBtn)
        rightControls.add(VerticalDivider(22))

        listOf(
            "Google" to "https://www.google.com",
            "GitHub" to "https://github.com/daviante-org/kromium",
            "Wikipedia" to "https://www.wikipedia.org",
            "WebGL Aquarium" to "https://webglsamples.org/aquarium/aquarium.html",
            "Acid3" to "http://acid3.acidtests.org/"
        ).forEach { (label, url) ->
            val presetBtn = KromiumButton(label, fontSize = 11f)
            presetBtn.preferredSize = Dimension(presetBtn.preferredSize.width, 34)
            presetBtn.addActionListener {
                navigateTo(url)
            }
            rightControls.add(presetBtn)
        }

        rightControls.add(VerticalDivider(22))

        toggleToolsBtn.preferredSize = Dimension(100, 34)
        toggleToolsBtn.addActionListener {
            isWorkbenchVisible = !isWorkbenchVisible
            toggleToolsBtn.text = if (isWorkbenchVisible) "Hide Tools ⇥" else "Show Tools ⇤"
            leftSidebarHolder.isVisible = isWorkbenchVisible
            rightWorkbenchPanel.isVisible = isWorkbenchVisible
            revalidate()
            repaint()
        }
        rightControls.add(toggleToolsBtn)

        toggleThemeBtn.preferredSize = Dimension(65, 34)
        toggleThemeBtn.addActionListener {
            SwingTheme.toggleTheme()
        }
        rightControls.add(toggleThemeBtn)

        bar.add(rightControls, BorderLayout.EAST)
        topContainer.add(bar)
        topContainer.add(HorizontalDivider())

        add(topContainer, BorderLayout.NORTH)
    }

    private fun setupMainSplit() {
        mainSplitPanel.isOpaque = false

        // 1. LEFT SIDEBAR: 180px
        leftSidebarPanel.layout = BoxLayout(leftSidebarPanel, BoxLayout.Y_AXIS)
        leftSidebarPanel.isOpaque = true
        leftSidebarPanel.background = SwingTheme.surface
        leftSidebarPanel.border = EmptyBorder(6, 6, 6, 6)
        leftSidebarPanel.preferredSize = Dimension(180, 100)

        val featuresHeader = KromiumLabel("FEATURES", isBold = true, fontSize = 10f)
        featuresHeader.isMuted = true
        featuresHeader.border = EmptyBorder(6, 8, 6, 8)
        leftSidebarPanel.add(featuresHeader)

        SampleFeature.values().forEach { feature ->
            val btnPanel = createFeatureSidebarItem(feature)
            featureSidebarButtons[feature] = btnPanel
            leftSidebarPanel.add(btnPanel)
            leftSidebarPanel.add(Box.createVerticalStrut(2))
        }
        leftSidebarPanel.add(Box.createVerticalGlue())

        leftSidebarHolder.isOpaque = false
        leftSidebarHolder.add(leftSidebarPanel, BorderLayout.CENTER)
        leftSidebarHolder.add(VerticalDivider(), BorderLayout.EAST)
        mainSplitPanel.add(leftSidebarHolder, BorderLayout.WEST)

        // 2. CENTER: Live Browser component
        centerBrowserHolder.isOpaque = false
        val awtComponent = browser.view.surface.unwrap(Component::class)
        if (awtComponent != null) {
            centerBrowserHolder.add(awtComponent, BorderLayout.CENTER)
        }
        mainSplitPanel.add(centerBrowserHolder, BorderLayout.CENTER)

        // 3. RIGHT PANEL: 360px
        rightWorkbenchPanel.isOpaque = true
        rightWorkbenchPanel.background = SwingTheme.surface
        rightWorkbenchPanel.preferredSize = Dimension(360, 100)

        val rightHolder = JPanel(BorderLayout())
        rightHolder.isOpaque = false
        rightHolder.add(VerticalDivider(), BorderLayout.WEST)
        rightHolder.add(rightWorkbenchPanel, BorderLayout.CENTER)

        mainSplitPanel.add(rightHolder, BorderLayout.EAST)
        add(mainSplitPanel, BorderLayout.CENTER)

        selectFeature(SampleFeature.NAVIGATION)
    }

    private fun createFeatureSidebarItem(feature: SampleFeature): JPanel {
        val item = object : JPanel(BorderLayout(8, 0)) {
            override fun paintComponent(g: Graphics) {
                if (feature == selectedFeature) {
                    val g2 = g.create() as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = SwingTheme.surfaceVariant
                    g2.fillRoundRect(0, 0, width - 1, height - 1, 6, 6)
                    g2.color = SwingTheme.border
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 6, 6)
                    g2.dispose()
                }
                super.paintComponent(g)
            }
        }
        item.isOpaque = false
        item.border = EmptyBorder(7, 8, 7, 8)
        item.maximumSize = Dimension(Int.MAX_VALUE, 32)
        item.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)

        val tagLbl = KromiumLabel(feature.iconTag, isBold = true, isMonospace = true, fontSize = 10f)
        val titleLbl = KromiumLabel(feature.title, isBold = (feature == selectedFeature), fontSize = 12f)

        item.add(tagLbl, BorderLayout.WEST)
        item.add(titleLbl, BorderLayout.CENTER)

        item.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                selectFeature(feature)
            }
        })

        return item
    }

    private fun selectFeature(feature: SampleFeature) {
        selectedFeature = feature
        featureSidebarButtons.values.forEach { it.repaint() }

        rightWorkbenchPanel.removeAll()
        val panel = featurePanels[feature]
        if (panel != null) {
            rightWorkbenchPanel.add(panel, BorderLayout.CENTER)
            if (feature == SampleFeature.DEVTOOLS && panel is DevToolsFeaturePanel) {
                panel.updateLogsView()
            }
        }
        rightWorkbenchPanel.revalidate()
        rightWorkbenchPanel.repaint()
    }

    fun onConsoleMessageAdded() {
        val panel = featurePanels[SampleFeature.DEVTOOLS]
        if (panel is DevToolsFeaturePanel && selectedFeature == SampleFeature.DEVTOOLS) {
            panel.updateLogsView()
        }
    }

    private fun navigateTo(url: String) {
        var target = url.trim()
        if (target.isNotEmpty()) {
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                target = "https://$target"
            }
            addressBar.text = target
            browser.navigation.loadUrl(target)
        }
    }

    private fun setupListeners() {
        addressBar.onEnter = { navigateTo(addressBar.text) }
        goBtn.addActionListener { navigateTo(addressBar.text) }

        backBtn.addActionListener { browser.navigation.goBack() }
        forwardBtn.addActionListener { browser.navigation.goForward() }
        reloadBtn.addActionListener { browser.navigation.reload(ignoreCache = false) }
        stopBtn.addActionListener { browser.navigation.stopLoad() }

        shellScope.launch {
            browser.navigation.navigationState.collectLatest { state ->
                if (!addressBar.hasFocus() && state.url.isNotBlank()) {
                    addressBar.text = state.url
                }

                val canBack = state.canGoBack || browser.navigation.canGoBack()
                val canForward = state.canGoForward || browser.navigation.canGoForward()

                backBtn.isEnabled = canBack
                forwardBtn.isEnabled = canForward

                if (state.isLoading) {
                    reloadBtn.isVisible = false
                    stopBtn.isVisible = true
                } else {
                    stopBtn.isVisible = false
                    reloadBtn.isVisible = true
                }
            }
        }
    }
}
