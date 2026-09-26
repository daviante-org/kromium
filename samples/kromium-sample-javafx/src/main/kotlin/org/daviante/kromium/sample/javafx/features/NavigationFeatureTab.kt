package org.daviante.kromium.sample.javafx.features

import org.daviante.kromium.api.core.KromiumBrowser
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class NavigationFeatureTab(private val browser: KromiumBrowser) : Tab("Navigation") {

    init {
        isClosable = false

        val contentBox = VBox(12.0).apply {
            padding = Insets(14.0)
            styleClass.add("card-panel")
        }

        // Section: Zoom & Scaling
        val zoomHeader = Label("Zoom & Rendering").apply { styleClass.add("section-header") }
        val zoomMuted = Label("Adjust Chromium viewport zoom factor and rendering filters.").apply { styleClass.add("muted-text") }

        val zoomSlider = Slider(-2.0, 4.0, 0.0).apply {
            majorTickUnit = 1.0
            minorTickCount = 1
            isSnapToTicks = false
            HBox.setHgrow(this, Priority.ALWAYS)
        }
        val zoomValueLabel = Label("100%").apply {
            styleClass.add("status-badge")
            minWidth = 50.0
            alignment = Pos.CENTER
        }

        zoomSlider.valueProperty().addListener { _, _, newVal ->
            val level = newVal.toDouble()
            browser.view.setZoom(level)
            val percent = ((level + 1.0) * 100).toInt()
            zoomValueLabel.text = "$percent%"
        }

        val resetZoomBtn = Button("Reset (100%)").apply {
            setOnAction {
                zoomSlider.value = 0.0
                browser.view.setZoom(0.0)
                zoomValueLabel.text = "100%"
            }
        }

        val zoomRow = HBox(10.0, zoomSlider, zoomValueLabel, resetZoomBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        val antialiasingCheck = CheckBox("Enable High-Quality Antialiasing").apply {
            isSelected = true
            setOnAction {
                browser.view.setAntialiasing(isSelected)
            }
        }

        // Section: Cache & Refresh Controls
        val cacheHeader = Label("Reload Controls").apply { styleClass.add("section-header") }
        val reloadNormalBtn = Button("Standard Reload").apply {
            setOnAction { browser.navigation.reload(ignoreCache = false) }
        }
        val reloadHardBtn = Button("Hard Reload (Ignore Cache)").apply {
            setOnAction { browser.navigation.reload(ignoreCache = true) }
        }
        val reloadRow = HBox(8.0, reloadNormalBtn, reloadHardBtn)

        // Section: Raw HTML Injection
        val htmlHeader = Label("Load Raw HTML").apply { styleClass.add("section-header") }
        val htmlMuted = Label("Inject custom HTML directly into the Chromium renderer engine.").apply { styleClass.add("muted-text") }
        val htmlTextArea = TextArea("""
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { background: #111827; color: #f3f4f6; font-family: sans-serif; padding: 40px; text-align: center; }
                h1 { color: #60a5fa; font-size: 32px; }
                p { font-size: 18px; color: #9ca3af; }
                .card { background: #1f2937; border-radius: 12px; padding: 24px; display: inline-block; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>⚡ Custom HTML Injected!</h1>
                <p>Rendered natively inside Kromium OSR via JavaFX SwingNode bridge.</p>
              </div>
            </body>
            </html>
        """.trimIndent()).apply {
            prefRowCount = 8
            styleClass.add("code-area")
        }

        val loadHtmlBtn = Button("Render HTML").apply {
            styleClass.add("button-primary")
            setOnAction {
                browser.navigation.loadHtml(htmlTextArea.text, "https://local.kromium.dev")
            }
        }

        contentBox.children.addAll(
            zoomHeader, zoomMuted, zoomRow, antialiasingCheck,
            Separator(),
            cacheHeader, reloadRow,
            Separator(),
            htmlHeader, htmlMuted, htmlTextArea, loadHtmlBtn
        )

        val scrollPane = ScrollPane(contentBox).apply {
            isFitToWidth = true
            style = "-fx-background-color: transparent;"
        }

        content = scrollPane
    }
}
