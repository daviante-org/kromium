package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.ui.KromiumBadge
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class NavigationFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private val panelScope = CoroutineScope(Dispatchers.Swing + Job())

    private val urlInput = KromiumTextField(placeholder = "URL...", fontSize = 12f)
    private val loadButton = KromiumButton(text = "Load", isPrimary = true, fontSize = 11f)

    private val backButton = KromiumButton(text = "Back", fontSize = 10f)
    private val forwardButton = KromiumButton(text = "Forward", fontSize = 10f)
    private val reloadButton = KromiumButton(text = "Reload", fontSize = 10f)
    private val hardReloadButton = KromiumButton(text = "Hard Reload", fontSize = 10f)
    private val stopButton = KromiumButton(text = "Stop", isPrimary = true, fontSize = 10f)

    private val loadingBadge = KromiumBadge(text = "IDLE", isActive = false)
    private val canBackBadge = KromiumBadge(text = "NO", isActive = false)
    private val canForwardBadge = KromiumBadge(text = "NO", isActive = false)

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.isOpaque = false

        // Header
        val titleLabel = KromiumLabel("Navigation & Session History", isBold = true, fontSize = 14f)
        val descLabel = KromiumLabel(
            "Fine-grained navigation control: navigate backwards/forwards in history, perform cache-busting hard reloads, or observe live state flows.",
            isMuted = true,
            fontSize = 11f
        )
        content.add(titleLabel)
        content.add(Box.createVerticalStrut(4))
        content.add(descLabel)
        content.add(Box.createVerticalStrut(12))

        // Card 1: URL input & actions
        val navCard = KromiumCard(padding = 10)
        navCard.layout = BoxLayout(navCard, BoxLayout.Y_AXIS)

        val inputRow = JPanel(BorderLayout(6, 0))
        inputRow.isOpaque = false
        inputRow.add(urlInput, BorderLayout.CENTER)
        inputRow.add(loadButton, BorderLayout.EAST)
        navCard.add(inputRow)
        navCard.add(Box.createVerticalStrut(8))

        val actionRow = JPanel(GridLayout(1, 5, 4, 0))
        actionRow.isOpaque = false
        actionRow.add(backButton)
        actionRow.add(forwardButton)
        actionRow.add(reloadButton)
        actionRow.add(hardReloadButton)
        actionRow.add(stopButton)
        navCard.add(actionRow)

        content.add(navCard)
        content.add(Box.createVerticalStrut(12))

        // Card 2: Quick presets
        val presetCard = KromiumCard(padding = 10)
        presetCard.layout = BoxLayout(presetCard, BoxLayout.Y_AXIS)
        val presetTitle = KromiumLabel("Presets:", isBold = true, fontSize = 11f)
        presetCard.add(presetTitle)
        presetCard.add(Box.createVerticalStrut(6))

        val presetRow = JPanel(GridLayout(1, 4, 4, 0))
        presetRow.isOpaque = false
        val presets = listOf(
            "https://example.com" to "Example",
            "https://wikipedia.org" to "Wikipedia",
            "https://github.com" to "GitHub",
            "https://browserleaks.com" to "BrowserLeaks"
        )
        presets.forEach { (url, label) ->
            val btn = KromiumButton(label, fontSize = 10f)
            btn.addActionListener {
                urlInput.text = url
                browser.navigation.loadUrl(url)
            }
            presetRow.add(btn)
        }
        presetCard.add(presetRow)

        content.add(presetCard)
        content.add(Box.createVerticalStrut(12))

        // Card 3: Live Navigation State
        val stateCard = KromiumCard(padding = 10)
        stateCard.layout = BoxLayout(stateCard, BoxLayout.Y_AXIS)
        val stateTitle = KromiumLabel("Live Navigation State", isBold = true, fontSize = 11f)
        stateCard.add(stateTitle)
        stateCard.add(Box.createVerticalStrut(8))

        val badgeRow = JPanel(GridLayout(1, 3, 8, 0))
        badgeRow.isOpaque = false

        badgeRow.add(createBadgeColumn("Loading", loadingBadge))
        badgeRow.add(createBadgeColumn("Can Go Back", canBackBadge))
        badgeRow.add(createBadgeColumn("Can Go Forward", canForwardBadge))
        stateCard.add(badgeRow)

        content.add(stateCard)
        content.add(Box.createVerticalGlue())

        add(content, BorderLayout.NORTH)

        setupListeners()
    }

    private fun createBadgeColumn(labelText: String, badge: KromiumBadge): JPanel {
        val col = JPanel()
        col.layout = BoxLayout(col, BoxLayout.Y_AXIS)
        col.isOpaque = false
        val lbl = KromiumLabel(labelText, isMuted = true, fontSize = 10f)
        lbl.alignmentX = 0.5f
        badge.alignmentX = 0.5f
        col.add(lbl)
        col.add(Box.createVerticalStrut(2))
        col.add(badge)
        return col
    }

    private fun setupListeners() {
        val performLoad = {
            var target = urlInput.text.trim()
            if (target.isNotEmpty()) {
                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                    target = "https://$target"
                }
                urlInput.text = target
                browser.navigation.loadUrl(target)
            }
        }

        urlInput.onEnter = performLoad
        loadButton.addActionListener { performLoad() }

        backButton.addActionListener { browser.navigation.goBack() }
        forwardButton.addActionListener { browser.navigation.goForward() }
        reloadButton.addActionListener { browser.navigation.reload(ignoreCache = false) }
        hardReloadButton.addActionListener { browser.navigation.reload(ignoreCache = true) }
        stopButton.addActionListener { browser.navigation.stopLoad() }

        panelScope.launch {
            browser.navigation.navigationState.collectLatest { state ->
                if (!urlInput.hasFocus() && state.url.isNotBlank()) {
                    urlInput.text = state.url
                }

                val canBack = state.canGoBack || browser.navigation.canGoBack()
                val canForward = state.canGoForward || browser.navigation.canGoForward()

                backButton.isEnabled = canBack
                forwardButton.isEnabled = canForward
                stopButton.isVisible = state.isLoading

                loadingBadge.setState(if (state.isLoading) "ACTIVE" else "IDLE", state.isLoading)
                canBackBadge.setState(if (canBack) "YES" else "NO", canBack)
                canForwardBadge.setState(if (canForward) "YES" else "NO", canForward)
            }
        }
    }
}
