package org.daviante.kromium.sample.javafx.features

import org.daviante.kromium.api.core.KromiumBrowser
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class DomExtractionFeatureTab(private val browser: KromiumBrowser) : Tab("DOM & Text") {

    init {
        isClosable = false

        val contentBox = VBox(12.0).apply {
            padding = Insets(14.0)
            styleClass.add("card-panel")
        }

        val header = Label("DOM & Content Extraction").apply { styleClass.add("section-header") }
        val muted = Label("Extract text or complete DOM HTML asynchronously without CSP restrictions.").apply {
            styleClass.add("muted-text")
        }

        val outputArea = TextArea().apply {
            isEditable = false
            prefRowCount = 16
            styleClass.add("code-area")
            promptText = "Extracted DOM or text content will appear here..."
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        val statusLabel = Label("Ready").apply { styleClass.add("muted-text") }

        val extractTextBtn = Button("Extract Text").apply {
            styleClass.add("button-primary")
            setOnAction {
                statusLabel.text = "Extracting text..."
                isDisable = true
                browser.jsBridge.getTextAsync().whenComplete { text, ex ->
                    Platform.runLater {
                        isDisable = false
                        if (ex != null) {
                            statusLabel.text = "Error: ${ex.message}"
                            outputArea.text = "Extraction failed: ${ex.message}"
                        } else {
                            val count = text?.length ?: 0
                            val words = text?.split(Regex("\\s+"))?.filter { it.isNotBlank() }?.size ?: 0
                            statusLabel.text = "Extracted $count characters (~$words words)"
                            outputArea.text = text ?: ""
                        }
                    }
                }
            }
        }

        val extractHtmlBtn = Button("Extract HTML (CSP-Immune)").apply {
            styleClass.add("button-accent")
            setOnAction {
                statusLabel.text = "Extracting HTML source..."
                isDisable = true
                browser.jsBridge.getHtmlAsync().whenComplete { html, ex ->
                    Platform.runLater {
                        isDisable = false
                        if (ex != null) {
                            statusLabel.text = "Error: ${ex.message}"
                            outputArea.text = "Extraction failed: ${ex.message}"
                        } else {
                            val bytes = html?.toByteArray()?.size ?: 0
                            statusLabel.text = "Extracted HTML (${bytes / 1024} KB)"
                            outputArea.text = html ?: ""
                        }
                    }
                }
            }
        }

        val copyBtn = Button("Copy Output").apply {
            setOnAction {
                val text = outputArea.text
                if (!text.isNullOrEmpty()) {
                    val clipboard = Clipboard.getSystemClipboard()
                    val content = ClipboardContent()
                    content.putString(text)
                    clipboard.setContent(content)
                    statusLabel.text = "Copied to clipboard!"
                }
            }
        }

        val clearBtn = Button("Clear").apply {
            setOnAction {
                outputArea.clear()
                statusLabel.text = "Cleared"
            }
        }

        val actionsRow = HBox(8.0, extractTextBtn, extractHtmlBtn, copyBtn, clearBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        contentBox.children.addAll(
            header, muted, actionsRow, statusLabel, outputArea
        )

        content = contentBox
    }
}
