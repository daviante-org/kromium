package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.print.KromiumPaperSize
import org.daviante.kromium.api.print.KromiumPdfMargins
import org.daviante.kromium.api.print.KromiumPdfSettings
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumChip
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumSwitch
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class PdfPrintFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private val defaultPdfPath = File(System.getProperty("user.home"), "Downloads${File.separator}kromium_export.pdf").absolutePath
    private val targetPathInput = KromiumTextField(placeholder = "Absolute PDF File Path", fontSize = 11f)

    private val resultCard = KromiumCard(BorderLayout(8, 0), padding = 8)
    private val resultLabel = KromiumLabel("", isBold = true, fontSize = 11f)
    private val resultSubLabel = KromiumLabel("", isMuted = true, fontSize = 10f)
    private val openPdfButton = KromiumButton("Open PDF", fontSize = 10f)
    private var generatedFile: File? = null

    private var selectedPaperSize = "A4"
    private val paperChips = mutableListOf<KromiumChip>()

    private var selectedMargin = "Default"
    private val marginChips = mutableListOf<KromiumChip>()

    private val landscapeSwitch = KromiumSwitch(checked = false)
    private val printBackgroundSwitch = KromiumSwitch(checked = true)

    private var scaleFactor = 1.0
    private val scaleReadout = KromiumLabel("100%", isBold = true, fontSize = 10f)
    private val scaleSlider = JSlider(30, 200, 100)

    private val pageRangesInput = KromiumTextField(placeholder = "Page Ranges (e.g. 1-3, 5)", fontSize = 11f)
    private val exportButton = KromiumButton("Export Page to PDF", isPrimary = true, fontSize = 12f)

    private val paperSizes = listOf(
        "A4" to KromiumPaperSize.A4,
        "Letter" to KromiumPaperSize.Letter,
        "Legal" to KromiumPaperSize.Legal,
        "Tabloid" to KromiumPaperSize.Tabloid
    )
    private val marginOptions = listOf(
        "Default" to KromiumPdfMargins.Default,
        "None" to KromiumPdfMargins.None,
        "Minimum" to KromiumPdfMargins.Minimum
    )

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        mainContainer.add(KromiumLabel("Vector PDF Printing & Export", isBold = true, fontSize = 14f))
        mainContainer.add(Box.createVerticalStrut(4))
        mainContainer.add(KromiumLabel(
            "Export the loaded DOM to sharp, searchable vector PDF documents with customizable paper sizes, orientation, CSS backgrounds, and margin specs.",
            isMuted = true,
            fontSize = 11f
        ))
        mainContainer.add(Box.createVerticalStrut(8))

        // Result Card
        resultCard.isVisible = false
        val resultTextCol = JPanel()
        resultTextCol.layout = BoxLayout(resultTextCol, BoxLayout.Y_AXIS)
        resultTextCol.isOpaque = false
        resultTextCol.add(resultLabel)
        resultTextCol.add(resultSubLabel)
        resultCard.add(resultTextCol, BorderLayout.CENTER)

        openPdfButton.preferredSize = Dimension(75, 26)
        openPdfButton.addActionListener {
            generatedFile?.let { file ->
                if (file.exists() && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(file)
                    } catch (e: Exception) {
                        resultLabel.text = "Could not open file: ${e.message}"
                    }
                }
            }
        }
        resultCard.add(openPdfButton, BorderLayout.EAST)
        mainContainer.add(resultCard)
        mainContainer.add(Box.createVerticalStrut(8))

        // Card 1: File destination
        val destCard = KromiumCard(padding = 10)
        destCard.layout = BoxLayout(destCard, BoxLayout.Y_AXIS)
        destCard.add(KromiumLabel("Destination Output Path", isBold = true, fontSize = 11f))
        destCard.add(Box.createVerticalStrut(6))
        targetPathInput.text = defaultPdfPath
        destCard.add(targetPathInput)
        mainContainer.add(destCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: Paper & Layout
        val layoutCard = KromiumCard(padding = 10)
        layoutCard.layout = BoxLayout(layoutCard, BoxLayout.Y_AXIS)
        layoutCard.add(KromiumLabel("Paper & Layout Configuration", isBold = true, fontSize = 11f))
        layoutCard.add(Box.createVerticalStrut(8))

        // Paper Dimensions
        layoutCard.add(KromiumLabel("Paper Dimensions:", isBold = true, fontSize = 10f))
        layoutCard.add(Box.createVerticalStrut(4))
        val paperRow = JPanel(GridLayout(1, 4, 4, 0))
        paperRow.isOpaque = false
        paperSizes.forEach { (name, _) ->
            val chip = KromiumChip(name, selected = (name == selectedPaperSize), fontSize = 10f)
            chip.addActionListener {
                paperChips.forEach { it.isSelected = false }
                chip.isSelected = true
                selectedPaperSize = name
            }
            paperChips.add(chip)
            paperRow.add(chip)
        }
        layoutCard.add(paperRow)
        layoutCard.add(Box.createVerticalStrut(8))

        // Margins
        layoutCard.add(KromiumLabel("Page Margins:", isBold = true, fontSize = 10f))
        layoutCard.add(Box.createVerticalStrut(4))
        val marginRow = JPanel(GridLayout(1, 3, 4, 0))
        marginRow.isOpaque = false
        marginOptions.forEach { (name, _) ->
            val chip = KromiumChip(name, selected = (name == selectedMargin), fontSize = 10f)
            chip.addActionListener {
                marginChips.forEach { it.isSelected = false }
                chip.isSelected = true
                selectedMargin = name
            }
            marginChips.add(chip)
            marginRow.add(chip)
        }
        layoutCard.add(marginRow)
        layoutCard.add(Box.createVerticalStrut(8))

        // Orientation & Backgrounds
        layoutCard.add(createSwitchRow("Landscape Orientation", "Default is Portrait", landscapeSwitch))
        layoutCard.add(Box.createVerticalStrut(6))
        layoutCard.add(createSwitchRow("Print CSS Backgrounds", "Renders colors and background images", printBackgroundSwitch))
        layoutCard.add(Box.createVerticalStrut(8))

        // Scale
        val scaleHeader = JPanel(BorderLayout())
        scaleHeader.isOpaque = false
        scaleHeader.add(KromiumLabel("Rendering Scale Factor", isBold = true, fontSize = 11f), BorderLayout.WEST)
        scaleHeader.add(scaleReadout, BorderLayout.EAST)
        layoutCard.add(scaleHeader)
        layoutCard.add(Box.createVerticalStrut(4))

        scaleSlider.isOpaque = false
        scaleSlider.addChangeListener {
            scaleFactor = scaleSlider.value / 100.0
            scaleReadout.text = "${scaleSlider.value}%"
        }
        layoutCard.add(scaleSlider)
        layoutCard.add(Box.createVerticalStrut(6))

        // Page ranges
        layoutCard.add(pageRangesInput)
        mainContainer.add(layoutCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Action Button
        exportButton.preferredSize = Dimension(100, 36)
        exportButton.addActionListener { exportToPdf() }
        mainContainer.add(exportButton)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)
    }

    private fun createSwitchRow(title: String, subtitle: String, toggle: KromiumSwitch): JPanel {
        val row = JPanel(BorderLayout(8, 0))
        row.isOpaque = false

        val textCol = JPanel()
        textCol.layout = BoxLayout(textCol, BoxLayout.Y_AXIS)
        textCol.isOpaque = false
        textCol.add(KromiumLabel(title, isBold = true, fontSize = 11f))
        textCol.add(KromiumLabel(subtitle, isMuted = true, fontSize = 10f))

        row.add(textCol, BorderLayout.CENTER)
        row.add(toggle, BorderLayout.EAST)
        return row
    }

    private fun exportToPdf() {
        val targetPath = targetPathInput.text.trim()
        if (targetPath.isEmpty()) return

        exportButton.isEnabled = false
        exportButton.text = "Rendering Vector PDF..."
        resultLabel.text = "Generating PDF..."
        resultSubLabel.text = ""
        resultCard.isVisible = true

        val file = File(targetPath)
        file.parentFile?.mkdirs()

        val resolvedPaperSize = paperSizes.first { it.first == selectedPaperSize }.second
        val resolvedMargins = marginOptions.first { it.first == selectedMargin }.second

        val settings = KromiumPdfSettings(
            landscape = landscapeSwitch.isChecked,
            printBackground = printBackgroundSwitch.isChecked,
            scale = scaleFactor,
            paperSize = resolvedPaperSize,
            margins = resolvedMargins,
            pageRanges = pageRangesInput.text.trim(),
            createDirectories = true
        )

        browser.view.printToPdf(targetPath, settings).thenAccept { success ->
            SwingUtilities.invokeLater {
                exportButton.isEnabled = true
                exportButton.text = "Export Page to PDF"
                if (success && file.exists()) {
                    resultLabel.text = "PDF successfully generated."
                    resultSubLabel.text = "Size: ${file.length() / 1024} KB | Path: ${file.name}"
                    generatedFile = file
                    openPdfButton.isVisible = true
                } else {
                    resultLabel.text = "PDF generation failed (file not written)."
                    resultSubLabel.text = ""
                    openPdfButton.isVisible = false
                }
            }
        }.exceptionally { ex ->
            SwingUtilities.invokeLater {
                exportButton.isEnabled = true
                exportButton.text = "Export Page to PDF"
                resultLabel.text = "PDF generation error: ${ex.message}"
                resultSubLabel.text = ""
                openPdfButton.isVisible = false
            }
            null
        }
    }
}
