package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumChip
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumSwitch
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class ViewToolsFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private var currentZoom = browser.view.zoomLevel
    private val zoomReadout = KromiumLabel("0.00 (approx 100%)", isMonospace = true, fontSize = 11f)
    private val zoomSlider = JSlider(-200, 200, (currentZoom * 100).toInt())

    private val aaSwitch = KromiumSwitch(checked = true)
    private val interpolationChips = mutableListOf<KromiumChip>()

    private val captureButton = KromiumButton("Capture", isPrimary = true, fontSize = 11f)
    private val screenshotInfo = KromiumLabel("", isMuted = true, fontSize = 10f)
    private var capturedImage: BufferedImage? = null
    private val imagePreviewPanel = object : JPanel() {
        init {
            isOpaque = false
            preferredSize = Dimension(100, 140)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val img = capturedImage ?: return
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

            val w = width
            val h = height
            val imgW = img.width
            val imgH = img.height

            val scale = minOf(w.toDouble() / imgW, h.toDouble() / imgH)
            val drawW = (imgW * scale).toInt()
            val drawH = (imgH * scale).toInt()
            val dx = (w - drawW) / 2
            val dy = (h - drawH) / 2

            g2.drawImage(img, dx, dy, drawW, drawH, null)
            g2.dispose()
        }
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        val titleLabel = KromiumLabel("View & Rendering Controls", isBold = true, fontSize = 14f)
        mainContainer.add(titleLabel)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 1: Zoom
        val zoomCard = KromiumCard(padding = 10)
        zoomCard.layout = BoxLayout(zoomCard, BoxLayout.Y_AXIS)

        val zoomHeader = JPanel(BorderLayout())
        zoomHeader.isOpaque = false
        zoomHeader.add(KromiumLabel("Zoom", isBold = true, fontSize = 11f), BorderLayout.WEST)
        zoomHeader.add(zoomReadout, BorderLayout.EAST)
        zoomCard.add(zoomHeader)
        zoomCard.add(Box.createVerticalStrut(6))

        zoomSlider.isOpaque = false
        zoomSlider.addChangeListener {
            val level = zoomSlider.value / 100.0
            currentZoom = level
            browser.view.setZoom(level)
            updateZoomReadout()
        }
        zoomCard.add(zoomSlider)
        zoomCard.add(Box.createVerticalStrut(6))

        val presetRow = JPanel(GridLayout(1, 5, 4, 0))
        presetRow.isOpaque = false
        val presets = listOf(-1.0 to "80%", 0.0 to "100%", 1.0 to "120%", 2.0 to "140%")
        presets.forEach { (level, label) ->
            val btn = KromiumButton(label, fontSize = 10f)
            btn.addActionListener {
                currentZoom = level
                zoomSlider.value = (level * 100).toInt()
                browser.view.setZoom(level)
                updateZoomReadout()
            }
            presetRow.add(btn)
        }
        val resetBtn = KromiumButton("Reset", isPrimary = true, fontSize = 10f)
        resetBtn.addActionListener {
            currentZoom = 0.0
            zoomSlider.value = 0
            browser.view.setZoom(0.0)
            updateZoomReadout()
        }
        presetRow.add(resetBtn)
        zoomCard.add(presetRow)

        mainContainer.add(zoomCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: Quality & Interpolation
        val qualityCard = KromiumCard(padding = 10)
        qualityCard.layout = BoxLayout(qualityCard, BoxLayout.Y_AXIS)

        qualityCard.add(KromiumLabel("Quality & Interpolation", isBold = true, fontSize = 11f))
        qualityCard.add(Box.createVerticalStrut(8))

        val aaRow = JPanel(BorderLayout())
        aaRow.isOpaque = false
        aaRow.add(KromiumLabel("Anti-Aliasing", fontSize = 11f), BorderLayout.WEST)
        aaSwitch.onCheckedChanged = { checked ->
            browser.view.setAntialiasing(checked)
        }
        aaRow.add(aaSwitch, BorderLayout.EAST)
        qualityCard.add(aaRow)
        qualityCard.add(Box.createVerticalStrut(8))

        qualityCard.add(KromiumLabel("Texture Interpolation:", isBold = true, fontSize = 10f))
        qualityCard.add(Box.createVerticalStrut(4))

        val chipRow = JPanel(GridLayout(1, 4, 4, 0))
        chipRow.isOpaque = false
        val modes = listOf("nearest_neighbor", "bilinear", "bicubic", "high")
        modes.forEach { mode ->
            val chip = KromiumChip(mode.replace("_", " "), selected = (mode == "bicubic"), fontSize = 10f)
            chip.addActionListener {
                interpolationChips.forEach { it.isSelected = false }
                chip.isSelected = true
                browser.view.setInterpolation(mode)
            }
            interpolationChips.add(chip)
            chipRow.add(chip)
        }
        qualityCard.add(chipRow)
        qualityCard.add(Box.createVerticalStrut(8))

        val dpiRow = JPanel(BorderLayout())
        dpiRow.isOpaque = false
        dpiRow.add(KromiumLabel("Auto-Calibrate DPI", fontSize = 11f), BorderLayout.WEST)
        val calibrateBtn = KromiumButton("Calibrate", fontSize = 10f)
        calibrateBtn.preferredSize = Dimension(70, 26)
        calibrateBtn.addActionListener {
            browser.view.resetScaleFactorToAuto()
        }
        dpiRow.add(calibrateBtn, BorderLayout.EAST)
        qualityCard.add(dpiRow)

        mainContainer.add(qualityCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 3: Screenshot
        val screenshotCard = KromiumCard(padding = 10)
        screenshotCard.layout = BoxLayout(screenshotCard, BoxLayout.Y_AXIS)

        val ssHeader = JPanel(BorderLayout())
        ssHeader.isOpaque = false
        ssHeader.add(KromiumLabel("Screenshot", isBold = true, fontSize = 11f), BorderLayout.WEST)
        captureButton.preferredSize = Dimension(65, 26)
        captureButton.addActionListener {
            captureButton.isEnabled = false
            captureButton.text = "..."
            browser.view.takeScreenshotAsync().thenAccept { bImg ->
                SwingUtilities.invokeLater {
                    captureButton.isEnabled = true
                    captureButton.text = "Capture"
                    if (bImg != null) {
                        capturedImage = bImg
                        screenshotInfo.text = "${bImg.width} \u00D7 ${bImg.height} px"
                        imagePreviewPanel.repaint()
                    } else {
                        screenshotInfo.text = "Capture failed (null)"
                    }
                }
            }.exceptionally { ex ->
                SwingUtilities.invokeLater {
                    captureButton.isEnabled = true
                    captureButton.text = "Capture"
                    screenshotInfo.text = "Error: ${ex.message}"
                }
                null
            }
        }
        ssHeader.add(captureButton, BorderLayout.EAST)
        screenshotCard.add(ssHeader)
        screenshotCard.add(Box.createVerticalStrut(4))
        screenshotCard.add(screenshotInfo)
        screenshotCard.add(Box.createVerticalStrut(6))

        val previewCard = KromiumCard(BorderLayout(), padding = 4)
        previewCard.add(imagePreviewPanel, BorderLayout.CENTER)
        screenshotCard.add(previewCard)

        mainContainer.add(screenshotCard)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)

        updateZoomReadout()
    }

    private fun updateZoomReadout() {
        val pct = (1.0 + currentZoom * 0.2) * 100
        zoomReadout.text = String.format("%.2f (approx %.0f%%)", currentZoom, pct)
    }
}
