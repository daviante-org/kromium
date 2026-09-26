package org.daviante.kromium.sample.javafx.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.javafx.model.ConsoleMessage
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class DevToolsConsoleFeatureTab(
    private val browser: KromiumBrowser,
    private val consoleLogs: ObservableList<ConsoleMessage>,
    private val onClearConsole: () -> Unit
) : Tab("DevTools & Console") {

    init {
        isClosable = false

        val contentBox = VBox(12.0).apply {
            padding = Insets(14.0)
            styleClass.add("card-panel")
        }

        val header = Label("Console & Developer Tools").apply { styleClass.add("section-header") }
        val muted = Label("Real-time Chromium console message stream and native DevTools inspector.").apply {
            styleClass.add("muted-text")
        }

        val openDevToolsBtn = Button("Open Native DevTools 🛠").apply {
            styleClass.add("button-accent")
            setOnAction {
                browser.devTools.openDevTools()
            }
        }

        val clearLogsBtn = Button("Clear Logs").apply {
            setOnAction {
                onClearConsole()
            }
        }

        val logCountLabel = Label("${consoleLogs.size} messages").apply {
            styleClass.add("status-badge")
        }

        consoleLogs.addListener(javafx.collections.ListChangeListener {
            logCountLabel.text = "${consoleLogs.size} messages"
        })

        val topActionRow = HBox(10.0, openDevToolsBtn, clearLogsBtn, logCountLabel).apply {
            alignment = Pos.CENTER_LEFT
        }

        val listView = ListView(consoleLogs).apply {
            setCellFactory {
                object : ListCell<ConsoleMessage>() {
                    override fun updateItem(item: ConsoleMessage?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (empty || item == null) {
                            text = null
                            graphic = null
                        } else {
                            val timeLabel = Label("[${item.timestamp}]").apply {
                                style = "-fx-text-fill: #8b949e; -fx-font-family: monospace; -fx-font-size: 11px;"
                            }
                            val locLabel = Label(if (item.source.isNotBlank()) "(${item.source.substringAfterLast('/')}:${item.line})" else "").apply {
                                style = "-fx-text-fill: #58a6ff; -fx-font-family: monospace; -fx-font-size: 11px;"
                            }
                            val msgLabel = Label(item.message).apply {
                                style = "-fx-text-fill: #e6edf3; -fx-font-family: monospace; -fx-font-size: 11px;"
                                isWrapText = true
                            }
                            graphic = VBox(2.0, HBox(6.0, timeLabel, locLabel), msgLabel).apply {
                                padding = Insets(4.0, 0.0, 4.0, 0.0)
                            }
                        }
                    }
                }
            }
            styleClass.add("code-area")
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        contentBox.children.addAll(header, muted, topActionRow, listView)
        content = contentBox
    }
}
