package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.model.ConsoleMessage
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class DevToolsFeaturePanel(
    private val browser: KromiumBrowser,
    private val consoleLogs: MutableList<ConsoleMessage>,
    private val onClearConsole: () -> Unit
) : JPanel() {

    private val panelScope = CoroutineScope(Dispatchers.Swing + Job())

    private val jsSnippetArea = JTextArea("console.log('Evaluated at ' + new Date().toISOString()); window.location.href")
    private val evalResultLabel = KromiumLabel("", isMuted = true, isMonospace = true, fontSize = 10f)
    private val evalButton = KromiumButton("Evaluate JS", isPrimary = true, fontSize = 11f)

    private val consoleHeaderLabel = KromiumLabel("Console Messages (0)", isBold = true, fontSize = 11f)
    private val filterInput = KromiumTextField(placeholder = "Filter console messages...", fontSize = 11f)
    private val logsContainer = JPanel()

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        mainContainer.add(KromiumLabel("Developer Tools & JS Console", isBold = true, fontSize = 14f))
        mainContainer.add(Box.createVerticalStrut(4))
        mainContainer.add(KromiumLabel(
            "Spawn Chromium's native DevTools inspector window, evaluate arbitrary JS snippets in the renderer context, and monitor console.log telemetry.",
            isMuted = true,
            fontSize = 11f
        ))
        mainContainer.add(Box.createVerticalStrut(8))

        // Card 1: Native DevTools Toggle
        val dtCard = KromiumCard(padding = 10)
        dtCard.layout = BorderLayout(8, 0)
        val dtTextCol = JPanel()
        dtTextCol.layout = BoxLayout(dtTextCol, BoxLayout.Y_AXIS)
        dtTextCol.isOpaque = false
        dtTextCol.add(KromiumLabel("Native Chromium DevTools", isBold = true, fontSize = 11f))
        dtTextCol.add(Box.createVerticalStrut(2))
        dtTextCol.add(KromiumLabel(
            "Launches the full Chrome Developer Tools window (DOM, Network, Console, Sources).",
            isMuted = true,
            fontSize = 10f
        ))
        dtCard.add(dtTextCol, BorderLayout.CENTER)

        val dtBtnRow = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        dtBtnRow.isOpaque = false
        val openDtBtn = KromiumButton("Open", isPrimary = true, fontSize = 10f)
        openDtBtn.preferredSize = Dimension(55, 26)
        openDtBtn.addActionListener { browser.devTools.openDevTools() }
        val closeDtBtn = KromiumButton("Close", fontSize = 10f)
        closeDtBtn.preferredSize = Dimension(55, 26)
        closeDtBtn.addActionListener { browser.devTools.closeDevTools() }
        dtBtnRow.add(openDtBtn)
        dtBtnRow.add(closeDtBtn)
        dtCard.add(dtBtnRow, BorderLayout.EAST)
        mainContainer.add(dtCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: JS Evaluator
        val evalCard = KromiumCard(padding = 10)
        evalCard.layout = BoxLayout(evalCard, BoxLayout.Y_AXIS)
        evalCard.add(KromiumLabel("Evaluate JavaScript in Renderer", isBold = true, fontSize = 11f))
        evalCard.add(Box.createVerticalStrut(6))

        jsSnippetArea.rows = 3
        jsSnippetArea.lineWrap = true
        jsSnippetArea.border = EmptyBorder(6, 6, 6, 6)
        val jsScroll = JScrollPane(jsSnippetArea)
        jsScroll.isOpaque = false
        jsScroll.viewport.isOpaque = false
        jsScroll.border = EmptyBorder(0, 0, 0, 0)
        evalCard.add(jsScroll)
        evalCard.add(Box.createVerticalStrut(6))

        val evalActionRow = JPanel(BorderLayout(8, 0))
        evalActionRow.isOpaque = false
        evalActionRow.add(evalResultLabel, BorderLayout.CENTER)

        evalButton.preferredSize = Dimension(95, 28)
        evalButton.addActionListener {
            val code = jsSnippetArea.text.trim()
            if (code.isNotEmpty()) {
                evalButton.isEnabled = false
                evalButton.text = "..."
                panelScope.launch {
                    try {
                        val res = browser.jsBridge.evaluateJavaScript(code)
                        evalResultLabel.text = "Result: ${res ?: "(null)"}"
                    } catch (e: Exception) {
                        evalResultLabel.text = "Error: ${e.message}"
                    } finally {
                        evalButton.isEnabled = true
                        evalButton.text = "Evaluate JS"
                    }
                }
            }
        }
        evalActionRow.add(evalButton, BorderLayout.EAST)
        evalCard.add(evalActionRow)
        mainContainer.add(evalCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 3: Console Logs
        val logsCard = KromiumCard(padding = 10)
        logsCard.layout = BoxLayout(logsCard, BoxLayout.Y_AXIS)

        val logsHeaderRow = JPanel(BorderLayout())
        logsHeaderRow.isOpaque = false
        logsHeaderRow.add(consoleHeaderLabel, BorderLayout.WEST)

        val clearLogsBtn = KromiumButton("Clear", fontSize = 10f)
        clearLogsBtn.preferredSize = Dimension(55, 22)
        clearLogsBtn.addActionListener {
            onClearConsole()
            updateLogsView()
        }
        logsHeaderRow.add(clearLogsBtn, BorderLayout.EAST)
        logsCard.add(logsHeaderRow)
        logsCard.add(Box.createVerticalStrut(6))

        filterInput.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                updateLogsView()
            }
        })
        logsCard.add(filterInput)
        logsCard.add(Box.createVerticalStrut(6))

        logsContainer.layout = BoxLayout(logsContainer, BoxLayout.Y_AXIS)
        logsContainer.isOpaque = false

        val logsScroll = JScrollPane(logsContainer)
        logsScroll.preferredSize = Dimension(100, 160)
        logsScroll.isOpaque = false
        logsScroll.viewport.isOpaque = false
        logsScroll.border = EmptyBorder(0, 0, 0, 0)
        logsCard.add(logsScroll)

        mainContainer.add(logsCard)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
        updateLogsView()
    }

    private fun updateColors() {
        jsSnippetArea.background = SwingTheme.surfaceVariant
        jsSnippetArea.foreground = SwingTheme.textPrimary
        jsSnippetArea.font = SwingTheme.fontMonospace(11f, false)
        updateLogsView()
    }

    fun updateLogsView() {
        val filter = filterInput.text.trim().lowercase()
        val filtered = if (filter.isEmpty()) {
            consoleLogs
        } else {
            consoleLogs.filter { it.message.lowercase().contains(filter) || it.source.lowercase().contains(filter) }
        }

        consoleHeaderLabel.text = "Console Messages (${consoleLogs.size})"
        logsContainer.removeAll()

        if (filtered.isEmpty()) {
            val emptyMsg = if (consoleLogs.isEmpty()) "No console messages emitted yet." else "No messages matching filter."
            val lbl = KromiumLabel(emptyMsg, isMuted = true, fontSize = 10f)
            lbl.horizontalAlignment = SwingConstants.CENTER
            logsContainer.add(lbl)
        } else {
            filtered.forEach { log ->
                logsContainer.add(createLogRow(log))
                logsContainer.add(Box.createVerticalStrut(3))
            }
        }
        logsContainer.revalidate()
        logsContainer.repaint()
    }

    private fun createLogRow(log: ConsoleMessage): JPanel {
        val row = KromiumCard(BorderLayout(6, 0), padding = 4)

        val textCol = JPanel()
        textCol.layout = BoxLayout(textCol, BoxLayout.Y_AXIS)
        textCol.isOpaque = false

        val msgLbl = KromiumLabel(log.message, isBold = false, isMonospace = true, fontSize = 10f)
        val srcLbl = KromiumLabel("${log.source}:${log.line}", isMuted = true, isMonospace = true, fontSize = 9f)
        textCol.add(msgLbl)
        textCol.add(srcLbl)
        row.add(textCol, BorderLayout.CENTER)

        val timeLbl = KromiumLabel(log.timestamp, isMuted = true, isMonospace = true, fontSize = 9f)
        row.add(timeLbl, BorderLayout.EAST)

        return row
    }
}
