package org.daviante.kromium.sample.swt.features

import org.daviante.kromium.sample.swt.model.ConsoleEntry
import org.daviante.kromium.sample.swt.ui.SwtTheme
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

class SwtConsolePanel(
    parent: Composite,
    private val theme: SwtTheme,
    private val onClearConsole: () -> Unit
) : Composite(parent, SWT.NONE) {

    private val allLogs = mutableListOf<ConsoleEntry>()
    private val table: Table
    private val filterInput: Text
    private val countLabel: Label

    init {
        layout = GridLayout(1, false)
        layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        background = theme.surface

        // Action Toolbar
        val topBar = Composite(this, SWT.NONE).apply {
            layout = GridLayout(5, false)
            layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
            background = theme.surface
        }

        Label(topBar, SWT.NONE).apply {
            text = "Filter:"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
        }

        filterInput = Text(topBar, SWT.BORDER or SWT.SEARCH).apply {
            message = "Filter logs..."
            layoutData = GridData(250, SWT.DEFAULT)
            font = theme.smallFont
            addModifyListener(ModifyListener { refreshTable() })
        }

        val clearBtn = Button(topBar, SWT.PUSH).apply {
            text = "Clear"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    allLogs.clear()
                    onClearConsole()
                    refreshTable()
                }
            })
        }

        val copyBtn = Button(topBar, SWT.PUSH).apply {
            text = "Copy Selected"
            font = theme.smallFont
            addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent?) {
                    val selection = table.selection
                    if (selection.isNotEmpty()) {
                        val text = selection.joinToString("\n") {
                            "[${it.getText(0)}] [${it.getText(1)}] ${it.getText(2)}: ${it.getText(3)}"
                        }
                        val clipboard = Clipboard(display)
                        clipboard.setContents(arrayOf(text), arrayOf(TextTransfer.getInstance() as Transfer))
                        clipboard.dispose()
                    }
                }
            })
        }

        countLabel = Label(topBar, SWT.NONE).apply {
            text = "0 logs"
            foreground = theme.textSecondary
            background = theme.surface
            font = theme.smallFont
            layoutData = GridData(SWT.END, SWT.CENTER, true, false)
        }

        // Table for Logs
        table = Table(this, SWT.BORDER or SWT.FULL_SELECTION or SWT.MULTI or SWT.V_SCROLL or SWT.H_SCROLL).apply {
            headerVisible = true
            linesVisible = true
            layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            font = theme.codeFont
        }

        val colTime = TableColumn(table, SWT.LEFT).apply {
            text = "Time"
            width = 85
        }
        val colLevel = TableColumn(table, SWT.LEFT).apply {
            text = "Level"
            width = 65
        }
        val colSource = TableColumn(table, SWT.LEFT).apply {
            text = "Source:Line"
            width = 180
        }
        val colMessage = TableColumn(table, SWT.LEFT).apply {
            text = "Message"
            width = 650
        }
    }

    fun addLog(entry: ConsoleEntry) {
        if (isDisposed) return
        allLogs.add(0, entry)
        if (allLogs.size > 1000) {
            allLogs.removeAt(allLogs.size - 1)
        }
        refreshTable()
    }

    private fun refreshTable() {
        if (isDisposed || table.isDisposed) return
        val filter = filterInput.text.trim().lowercase()
        val filtered = if (filter.isEmpty()) {
            allLogs
        } else {
            allLogs.filter {
                it.message.lowercase().contains(filter) ||
                it.source.lowercase().contains(filter) ||
                it.level.lowercase().contains(filter)
            }
        }

        table.setRedraw(false)
        table.removeAll()
        for (log in filtered) {
            val item = TableItem(table, SWT.NONE)
            item.setText(0, log.timestamp)
            item.setText(1, log.level)
            item.setText(2, if (log.line > 0) "${log.source}:${log.line}" else log.source)
            item.setText(3, log.message)

            if (log.level == "ERROR") {
                item.foreground = theme.error
            }
        }
        table.setRedraw(true)
        countLabel.text = "${filtered.size} of ${allLogs.size} logs"
    }
}
