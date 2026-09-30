package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.model.AutomationAction
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumChip
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class AutomationFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private var selectedAction = AutomationAction.COUNT
    private val actionChips = mutableListOf<KromiumChip>()

    private val selectorInput = KromiumTextField(placeholder = "CSS Selector (e.g. h1, button, a)", fontSize = 11f)
    private val argInput = KromiumTextField(placeholder = "Argument (e.g. text or attribute name)", fontSize = 11f)
    private val executeButton = KromiumButton("Execute", isPrimary = true, fontSize = 11f)

    private val logArea = JTextArea()
    private val logHeader = KromiumLabel("Activity Log (0)", isBold = true, fontSize = 10f)
    private val logEntries = mutableListOf<String>()

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        mainContainer.add(KromiumLabel("Playwright DOM Automation", isBold = true, fontSize = 14f))
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 1: Desktop Emulation
        val emuCard = KromiumCard(padding = 10)
        emuCard.layout = BorderLayout(8, 0)
        val emuTextCol = JPanel()
        emuTextCol.layout = BoxLayout(emuTextCol, BoxLayout.Y_AXIS)
        emuTextCol.isOpaque = false
        emuTextCol.add(KromiumLabel("Desktop Emulation", isBold = true, fontSize = 11f))
        emuTextCol.add(Box.createVerticalStrut(2))
        emuTextCol.add(KromiumLabel("Injects scripts to spoof desktop screen & navigator properties.", isMuted = true, fontSize = 10f))
        emuCard.add(emuTextCol, BorderLayout.CENTER)

        val emuBtn = KromiumButton("Emulate", fontSize = 10f)
        emuBtn.preferredSize = Dimension(65, 26)
        emuBtn.addActionListener {
            browser.automation.emulateDesktopEnvironment()
            addLog("Emulation script dispatched.")
        }
        emuCard.add(emuBtn, BorderLayout.EAST)
        mainContainer.add(emuCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: Query Runner
        val queryCard = KromiumCard(padding = 10)
        queryCard.layout = BoxLayout(queryCard, BoxLayout.Y_AXIS)
        queryCard.add(KromiumLabel("Query Runner", isBold = true, fontSize = 11f))
        queryCard.add(Box.createVerticalStrut(6))

        // Action chips grid
        val chipsRow1 = JPanel(GridLayout(1, 5, 4, 0))
        chipsRow1.isOpaque = false
        val chipsRow2 = JPanel(GridLayout(1, 4, 4, 0))
        chipsRow2.isOpaque = false

        val allActions = AutomationAction.values()
        allActions.forEachIndexed { index, action ->
            val chip = KromiumChip(action.label, selected = (action == selectedAction), fontSize = 10f)
            chip.addActionListener {
                actionChips.forEach { it.isSelected = false }
                chip.isSelected = true
                selectedAction = action
                updateArgInputVisibility()
            }
            actionChips.add(chip)
            if (index < 5) chipsRow1.add(chip) else chipsRow2.add(chip)
        }
        queryCard.add(chipsRow1)
        queryCard.add(Box.createVerticalStrut(4))
        queryCard.add(chipsRow2)
        queryCard.add(Box.createVerticalStrut(8))

        selectorInput.text = "input, button, h1, a"
        queryCard.add(selectorInput)
        queryCard.add(Box.createVerticalStrut(6))

        argInput.isVisible = false
        queryCard.add(argInput)
        queryCard.add(Box.createVerticalStrut(6))

        val execRow = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        execRow.isOpaque = false
        executeButton.preferredSize = Dimension(80, 28)
        executeButton.addActionListener { executeAction() }
        execRow.add(executeButton)
        queryCard.add(execRow)

        mainContainer.add(queryCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 3: Activity Log
        val logCard = KromiumCard(padding = 10)
        logCard.layout = BoxLayout(logCard, BoxLayout.Y_AXIS)

        val logHeaderRow = JPanel(BorderLayout())
        logHeaderRow.isOpaque = false
        logHeaderRow.add(logHeader, BorderLayout.WEST)

        val clearLogBtn = KromiumButton("Clear", fontSize = 10f)
        clearLogBtn.preferredSize = Dimension(50, 22)
        clearLogBtn.addActionListener {
            logEntries.clear()
            logArea.text = "No actions executed yet."
            logHeader.text = "Activity Log (0)"
        }
        logHeaderRow.add(clearLogBtn, BorderLayout.EAST)
        logCard.add(logHeaderRow)
        logCard.add(Box.createVerticalStrut(6))

        logArea.isEditable = false
        logArea.border = EmptyBorder(6, 6, 6, 6)
        logArea.text = "No actions executed yet."
        val logScroll = JScrollPane(logArea)
        logScroll.preferredSize = Dimension(100, 130)
        logScroll.isOpaque = false
        logScroll.viewport.isOpaque = false
        logScroll.border = EmptyBorder(0, 0, 0, 0)
        logCard.add(logScroll)

        mainContainer.add(logCard)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
    }

    private fun updateColors() {
        logArea.background = SwingTheme.surfaceVariant
        logArea.foreground = SwingTheme.textPrimary
        logArea.font = SwingTheme.fontMonospace(10f, false)
    }

    private fun updateArgInputVisibility() {
        val needsArg = selectedAction in listOf(AutomationAction.FILL, AutomationAction.TYPE, AutomationAction.GET_ATTRIBUTE)
        argInput.isVisible = needsArg
        if (needsArg) {
            argInput.placeholder = when (selectedAction) {
                AutomationAction.GET_ATTRIBUTE -> "Attribute name (e.g. href, src)"
                AutomationAction.FILL -> "Value to fill"
                AutomationAction.TYPE -> "Text to type"
                else -> "Argument"
            }
        }
        revalidate()
        repaint()
    }

    private fun addLog(entry: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logLine = "[$time] $entry"
        logEntries.add(0, logLine)
        logArea.text = logEntries.joinToString("\n")
        logHeader.text = "Activity Log (${logEntries.size})"
    }

    private fun executeAction() {
        val targetSelector = selectorInput.text.trim()
        if (targetSelector.isEmpty()) return

        val arg = argInput.text.trim()
        executeButton.isEnabled = false
        executeButton.text = "..."

        val finish = {
            SwingUtilities.invokeLater {
                executeButton.isEnabled = true
                executeButton.text = "Execute"
            }
        }

        when (selectedAction) {
            AutomationAction.WAIT_FOR -> {
                browser.automation.waitForSelectorAsync(targetSelector).thenAccept { found ->
                    SwingUtilities.invokeLater { addLog("waitForSelector('$targetSelector') -> $found") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("waitForSelector ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.CLICK -> {
                browser.automation.clickAsync(targetSelector).thenAccept { clicked ->
                    SwingUtilities.invokeLater { addLog("click('$targetSelector') -> $clicked") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("click ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.FILL -> {
                browser.automation.fillAsync(targetSelector, arg).thenAccept { filled ->
                    SwingUtilities.invokeLater { addLog("fill('$targetSelector', '$arg') -> $filled") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("fill ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.TYPE -> {
                browser.automation.typeAsync(targetSelector, arg).thenAccept { typed ->
                    SwingUtilities.invokeLater { addLog("type('$targetSelector', '$arg') -> $typed") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("type ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.GET_TEXT -> {
                browser.automation.getTextContentAsync(targetSelector).thenAccept { text ->
                    SwingUtilities.invokeLater { addLog("getText('$targetSelector') -> \"$text\"") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("getText ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.GET_ATTRIBUTE -> {
                browser.automation.getAttributeAsync(targetSelector, arg).thenAccept { attr ->
                    SwingUtilities.invokeLater { addLog("getAttr('$targetSelector', '$arg') -> \"$attr\"") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("getAttr ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.IS_VISIBLE -> {
                browser.automation.isVisibleAsync(targetSelector).thenAccept { visible ->
                    SwingUtilities.invokeLater { addLog("isVisible('$targetSelector') -> $visible") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("isVisible ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.IS_CHECKED -> {
                browser.automation.isCheckedAsync(targetSelector).thenAccept { checked ->
                    SwingUtilities.invokeLater { addLog("isChecked('$targetSelector') -> $checked") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("isChecked ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
            AutomationAction.COUNT -> {
                browser.automation.countAsync(targetSelector).thenAccept { count ->
                    SwingUtilities.invokeLater { addLog("count('$targetSelector') -> $count") }
                    finish()
                }.exceptionally { ex ->
                    SwingUtilities.invokeLater { addLog("count ERROR: ${ex.message}") }
                    finish()
                    null
                }
            }
        }
    }
}
