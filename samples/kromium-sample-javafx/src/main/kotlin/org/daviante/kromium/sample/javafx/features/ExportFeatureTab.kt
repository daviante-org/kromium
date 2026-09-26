package org.daviante.kromium.sample.javafx.features

import org.daviante.kromium.api.core.KromiumBrowser
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ExportFeatureTab(private val browser: KromiumBrowser) : Tab("Capture & PDF") {

    private var lastCapturedImage: BufferedImage? = null

    init {
        isClosable = false

        val contentBox = VBox(12.0).apply {
            padding = Insets(14.0)
            styleClass.add("card-panel")
        }

        // --- SECTION 1: Native Screenshot Capture ---
        val screenHeader = Label("Native Screenshot Capture").apply { styleClass.add("section-header") }
        val screenMuted = Label("Capture the OSR front-buffer asynchronously without OS window obstruction.").apply {
            styleClass.add("muted-text")
        }

        val imageView = ImageView().apply {
            isPreserveRatio = true
            fitWidth = 340.0
            fitHeight = 200.0
            style = "-fx-border-color: #30363d; -fx-border-width: 1; -fx-background-color: #090d13;"
        }

        val screenStatus = Label("No screenshot captured yet").apply { styleClass.add("muted-text") }

        val captureBtn = Button("Capture Screenshot 📸").apply {
            styleClass.add("button-primary")
            setOnAction {
                screenStatus.text = "Capturing viewport..."
                isDisable = true
                browser.view.takeScreenshotAsync().whenComplete { img, ex ->
                    Platform.runLater {
                        isDisable = false
                        if (ex != null) {
                            screenStatus.text = "Error: ${ex.message}"
                        } else if (img != null) {
                            lastCapturedImage = img
                            val fxImg = WritableImage(img.width, img.height)
                            val pixels = IntArray(img.width * img.height)
                            img.getRGB(0, 0, img.width, img.height, pixels, 0, img.width)
                            fxImg.pixelWriter.setPixels(0, 0, img.width, img.height, PixelFormat.getIntArgbInstance(), pixels, 0, img.width)
                            imageView.image = fxImg
                            screenStatus.text = "Captured ${img.width}x${img.height} px"
                        } else {
                            screenStatus.text = "Failed to capture image (empty buffer)"
                        }
                    }
                }
            }
        }

        val saveImgBtn = Button("Save Image...").apply {
            setOnAction {
                val img = lastCapturedImage
                if (img != null) {
                    try {
                        val file = File(System.getProperty("user.home"), "kromium_screenshot.png")
                        ImageIO.write(img, "png", file)
                        screenStatus.text = "Saved to: ${file.absolutePath}"
                    } catch (t: Throwable) {
                        screenStatus.text = "Failed to save: ${t.message}"
                    }
                } else {
                    screenStatus.text = "Capture a screenshot first"
                }
            }
        }

        val screenActions = HBox(8.0, captureBtn, saveImgBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        // --- SECTION 2: Vector PDF Generation ---
        val pdfHeader = Label("Vector PDF Export").apply { styleClass.add("section-header") }
        val pdfMuted = Label("Generate print-quality vector PDF via Chromium's native print engine.").apply {
            styleClass.add("muted-text")
        }

        val defaultPdfPath = File(System.getProperty("user.home"), "kromium_export.pdf").absolutePath
        val pdfPathField = TextField(defaultPdfPath).apply {
            promptText = "Target PDF absolute path..."
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val pdfStatus = Label("Ready to export").apply { styleClass.add("muted-text") }

        val printPdfBtn = Button("Generate PDF 📄").apply {
            styleClass.add("button-accent")
            setOnAction {
                val path = pdfPathField.text.trim()
                if (path.isNotBlank()) {
                    pdfStatus.text = "Generating vector PDF..."
                    isDisable = true
                    browser.view.printToPdf(path).whenComplete { ok, ex ->
                        Platform.runLater {
                            isDisable = false
                            if (ex != null) {
                                pdfStatus.text = "PDF export failed: ${ex.message}"
                            } else if (ok) {
                                val file = File(path)
                                val sizeKb = if (file.exists()) file.length() / 1024 else 0
                                pdfStatus.text = "PDF exported successfully ($sizeKb KB): ${file.name}"
                            } else {
                                pdfStatus.text = "PDF generation returned false"
                            }
                        }
                    }
                }
            }
        }

        val pdfActions = HBox(8.0, printPdfBtn, pdfStatus).apply {
            alignment = Pos.CENTER_LEFT
        }

        contentBox.children.addAll(
            screenHeader, screenMuted, screenActions, screenStatus, imageView,
            Separator(),
            pdfHeader, pdfMuted, pdfPathField, pdfActions
        )

        val scrollPane = ScrollPane(contentBox).apply {
            isFitToWidth = true
            style = "-fx-background-color: transparent;"
        }

        content = scrollPane
    }
}
