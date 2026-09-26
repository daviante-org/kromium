package org.daviante.kromium.sample.javafx.features

import org.daviante.kromium.api.core.KromiumBrowser
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch

class AutomationFeatureTab(
    private val browser: KromiumBrowser,
    private val scope: CoroutineScope
) : Tab("Automation") {

    init {
        isClosable = false

        val contentBox = VBox(12.0).apply {
            padding = Insets(14.0)
            styleClass.add("card-panel")
        }

        // --- SECTION 1: JavaScript REPL ---
        val jsHeader = Label("JavaScript REPL").apply { styleClass.add("section-header") }
        val jsMuted = Label("Execute expressions with live evaluation and safe IIFE encapsulation.").apply {
            styleClass.add("muted-text")
        }

        val jsInputArea = TextArea("document.title").apply {
            prefRowCount = 3
            styleClass.add("code-area")
        }

        // Quick Presets
        val presetsRow = HBox(6.0).apply {
            alignment = Pos.CENTER_LEFT
            val presets = listOf(
                "title" to "document.title",
                "href" to "window.location.href",
                "UA" to "navigator.userAgent",
                "viewport" to "({ width: window.innerWidth, height: window.innerHeight })",
                "links count" to "document.querySelectorAll('a').length"
            )
            presets.forEach { (name, code) ->
                val chip = Button(name).apply {
                    styleClass.add("chip-button")
                    setOnAction { jsInputArea.text = code }
                }
                children.add(chip)
            }
        }

        val jsResultArea = TextArea().apply {
            isEditable = false
            prefRowCount = 3
            promptText = "Result or error will appear here..."
            styleClass.add("code-area")
        }

        val evalStatus = Label("Ready").apply { styleClass.add("muted-text") }

        val runJsBtn = Button("Run Script ▶").apply {
            styleClass.add("button-primary")
            setOnAction {
                val code = jsInputArea.text
                if (code.isNotBlank()) {
                    evalStatus.text = "Evaluating..."
                    scope.launch {
                        try {
                            val result = browser.jsBridge.evaluateJavaScript(code)
                            evalStatus.text = "Success"
                            jsResultArea.text = result ?: "(null / undefined)"
                        } catch (t: Throwable) {
                            evalStatus.text = "Evaluation failed"
                            jsResultArea.text = "Error: ${t.message}"
                        }
                    }
                }
            }
        }

        val jsActionRow = HBox(10.0, runJsBtn, evalStatus).apply {
            alignment = Pos.CENTER_LEFT
        }

        // --- SECTION 2: DOM Automation ---
        val domHeader = Label("DOM Automation Actions").apply { styleClass.add("section-header") }
        val domMuted = Label("Playwright-style locator actions (click, type, fill, count).").apply {
            styleClass.add("muted-text")
        }

        val selectorField = TextField().apply {
            promptText = "CSS Selector (e.g. input[name='q'], a, button)..."
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val valueField = TextField().apply {
            promptText = "Value / text to type..."
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val domStatus = Label("Idle").apply { styleClass.add("muted-text") }

        val clickBtn = Button("Click Element").apply {
            setOnAction {
                val sel = selectorField.text
                if (sel.isNotBlank()) {
                    domStatus.text = "Clicking '$sel'..."
                    browser.automation.clickAsync(sel).whenComplete { ok, ex ->
                        Platform.runLater {
                            domStatus.text = if (ex != null) "Error: ${ex.message}" else if (ok) "Clicked '$sel' successfully!" else "Element '$sel' not found"
                        }
                    }
                }
            }
        }

        val fillBtn = Button("Fill Text").apply {
            setOnAction {
                val sel = selectorField.text
                val text = valueField.text
                if (sel.isNotBlank()) {
                    domStatus.text = "Filling '$sel'..."
                    browser.automation.fillAsync(sel, text).whenComplete { ok, ex ->
                        Platform.runLater {
                            domStatus.text = if (ex != null) "Error: ${ex.message}" else if (ok) "Filled '$sel' with text" else "Element not found"
                        }
                    }
                }
            }
        }

        val countBtn = Button("Count Elements").apply {
            setOnAction {
                val sel = selectorField.text
                if (sel.isNotBlank()) {
                    domStatus.text = "Counting '$sel'..."
                    browser.automation.countAsync(sel).whenComplete { count, ex ->
                        Platform.runLater {
                            domStatus.text = if (ex != null) "Error: ${ex.message}" else "Found $count matching elements for '$sel'"
                        }
                    }
                }
            }
        }

        val getTextBtn = Button("Get Element Text").apply {
            setOnAction {
                val sel = selectorField.text
                if (sel.isNotBlank()) {
                    domStatus.text = "Reading '$sel' text..."
                    browser.automation.getTextContentAsync(sel).whenComplete { text, ex ->
                        Platform.runLater {
                            domStatus.text = if (ex != null) "Error: ${ex.message}" else "Text: \"$text\""
                        }
                    }
                }
            }
        }

        val domBtnRow1 = HBox(8.0, clickBtn, fillBtn, countBtn, getTextBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        contentBox.children.addAll(
            jsHeader, jsMuted, presetsRow, jsInputArea, jsActionRow, jsResultArea,
            Separator(),
            domHeader, domMuted, selectorField, valueField, domBtnRow1, domStatus
        )

        val scrollPane = ScrollPane(contentBox).apply {
            isFitToWidth = true
            style = "-fx-background-color: transparent;"
        }

        content = scrollPane
    }
}
