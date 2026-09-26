package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.SwingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.border.EmptyBorder

class DomExtractionFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private val panelScope = CoroutineScope(Dispatchers.Swing + Job())

    private val htmlButton = KromiumButton("HTML", isPrimary = true, fontSize = 11f)
    private val textButton = KromiumButton("Text", isPrimary = true, fontSize = 11f)
    private val titleButton = KromiumButton("Title", fontSize = 11f)
    private val faviconButton = KromiumButton("Favicon", fontSize = 11f)

    private val metaLabel = KromiumLabel("No extraction performed yet.", isMuted = true, isMonospace = true, fontSize = 10f)
    private val copyButton = KromiumButton("Copy", fontSize = 10f)

    private val contentArea = JTextArea()
    private val scrollPane = JScrollPane(contentArea)

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.isOpaque = false

        val titleLabel = KromiumLabel("DOM & Content Extraction", isBold = true, fontSize = 14f)
        val descLabel = KromiumLabel(
            "Asynchronously extracts DOM trees, text, and metadata from the live Chromium frame without blocking.",
            isMuted = true,
            fontSize = 11f
        )
        topPanel.add(titleLabel)
        topPanel.add(Box.createVerticalStrut(4))
        topPanel.add(descLabel)
        topPanel.add(Box.createVerticalStrut(10))

        // Trigger buttons
        val buttonRow = JPanel(GridLayout(1, 4, 4, 0))
        buttonRow.isOpaque = false
        buttonRow.add(htmlButton)
        buttonRow.add(textButton)
        buttonRow.add(titleButton)
        buttonRow.add(faviconButton)
        topPanel.add(buttonRow)
        topPanel.add(Box.createVerticalStrut(10))

        // Status row
        val statusRow = JPanel(BorderLayout(8, 0))
        statusRow.isOpaque = false
        statusRow.add(metaLabel, BorderLayout.CENTER)
        copyButton.preferredSize = Dimension(60, 26)
        statusRow.add(copyButton, BorderLayout.EAST)
        topPanel.add(statusRow)
        topPanel.add(Box.createVerticalStrut(8))

        add(topPanel, BorderLayout.NORTH)

        // Text area setup
        contentArea.isEditable = false
        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true
        contentArea.border = EmptyBorder(8, 8, 8, 8)
        contentArea.text = "Select an extraction target above to inspect live DOM output."

        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        scrollPane.border = EmptyBorder(0, 0, 0, 0)

        val cardPanel = KromiumCard(BorderLayout(), padding = 4)
        cardPanel.add(scrollPane, BorderLayout.CENTER)
        add(cardPanel, BorderLayout.CENTER)

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
        setupActions()
    }

    private fun updateColors() {
        contentArea.background = SwingTheme.surfaceVariant
        contentArea.foreground = SwingTheme.textPrimary
        contentArea.font = SwingTheme.fontMonospace(11f, false)
    }

    private fun setupActions() {
        copyButton.addActionListener {
            val text = contentArea.text
            if (text.isNotBlank()) {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                metaLabel.text = "Copied to clipboard."
            }
        }

        htmlButton.addActionListener {
            setExtracting(true)
            panelScope.launch {
                val start = System.currentTimeMillis()
                val html = browser.jsBridge.getHtml()
                val elapsed = System.currentTimeMillis() - start
                contentArea.text = html
                metaLabel.text = "outerHTML: ${html.length} chars in ${elapsed}ms"
                setExtracting(false)
            }
        }

        textButton.addActionListener {
            setExtracting(true)
            panelScope.launch {
                val start = System.currentTimeMillis()
                val text = browser.jsBridge.getText()
                val elapsed = System.currentTimeMillis() - start
                contentArea.text = text
                metaLabel.text = "body.innerText: ${text.length} chars in ${elapsed}ms"
                setExtracting(false)
            }
        }

        titleButton.addActionListener {
            setExtracting(true)
            panelScope.launch {
                val start = System.currentTimeMillis()
                val title = browser.jsBridge.evaluateJavaScript("document.title") ?: ""
                val elapsed = System.currentTimeMillis() - start
                contentArea.text = title
                metaLabel.text = "title in ${elapsed}ms"
                setExtracting(false)
            }
        }

        faviconButton.addActionListener {
            setExtracting(true)
            panelScope.launch {
                val start = System.currentTimeMillis()
                val favicon = browser.jsBridge.getFaviconUrl() ?: "No favicon found"
                val elapsed = System.currentTimeMillis() - start
                contentArea.text = favicon
                metaLabel.text = "favicon in ${elapsed}ms"
                setExtracting(false)
            }
        }
    }

    private fun setExtracting(extracting: Boolean) {
        htmlButton.isEnabled = !extracting
        textButton.isEnabled = !extracting
        titleButton.isEnabled = !extracting
        faviconButton.isEnabled = !extracting
        if (extracting) {
            metaLabel.text = "Extracting..."
        }
    }
}
