package org.daviante.kromium.sample.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.print.KromiumPaperSize
import org.daviante.kromium.api.print.KromiumPdfMargins
import org.daviante.kromium.api.print.KromiumPdfSettings
import org.daviante.kromium.sample.ui.KromiumInput
import java.awt.Desktop
import java.io.File

@Composable
fun PdfPrintFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    val defaultPdfPath = remember {
        val userHome = System.getProperty("user.home")
        File(userHome, "Downloads${File.separator}kromium_export.pdf").absolutePath
    }

    var targetPath by remember { mutableStateOf(defaultPdfPath) }
    var isLandscape by remember { mutableStateOf(false) }
    var printBackground by remember { mutableStateOf(true) }
    var scaleFactor by remember { mutableStateOf(1.0) }
    var selectedPaperSize by remember { mutableStateOf("A4") }
    var selectedMargin by remember { mutableStateOf("Default") }
    var pageRanges by remember { mutableStateOf("") }
    var isPrinting by remember { mutableStateOf(false) }
    var printResult by remember { mutableStateOf<String?>(null) }
    var generatedFile by remember { mutableStateOf<File?>(null) }

    val paperSizes = listOf("A4" to KromiumPaperSize.A4, "Letter" to KromiumPaperSize.Letter, "Legal" to KromiumPaperSize.Legal, "Tabloid" to KromiumPaperSize.Tabloid)
    val marginOptions = listOf("Default" to KromiumPdfMargins.Default, "None" to KromiumPdfMargins.None, "Minimum" to KromiumPdfMargins.Minimum)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vector PDF Printing & Export",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Export the loaded DOM to sharp, searchable vector PDF documents with customizable paper sizes, orientation, CSS backgrounds, and margin specs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        printResult?.let { resultMsg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resultMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        generatedFile?.let { file ->
                            if (file.exists()) {
                                Text(
                                    text = "File size: ${file.length() / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (generatedFile != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Button(
                            onClick = {
                                try {
                                    Desktop.getDesktop().open(generatedFile)
                                } catch (e: Exception) {
                                    printResult = "Could not open file: ${e.message}"
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Open PDF", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Section: File Path Destination
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Destination Output Path",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                KromiumInput(
                    value = targetPath,
                    onValueChange = { targetPath = it },
                    placeholder = "Absolute PDF File Path",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section: Paper & Geometry
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paper & Layout Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Paper Size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Paper Dimensions:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        paperSizes.forEach { (name, _) ->
                            FilterChip(
                                selected = selectedPaperSize == name,
                                onClick = { selectedPaperSize = name },
                                label = { Text(name, fontSize = 10.sp) },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Margins
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Page Margins:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        marginOptions.forEach { (name, _) ->
                            FilterChip(
                                selected = selectedMargin == name,
                                onClick = { selectedMargin = name },
                                label = { Text(name, fontSize = 10.sp) },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Orientation & Backgrounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Landscape Orientation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Default is Portrait", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isLandscape,
                        onCheckedChange = { isLandscape = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Print CSS Backgrounds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Renders colors and background images", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = printBackground,
                        onCheckedChange = { printBackground = it }
                    )
                }

                HorizontalDivider()

                // Scale Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rendering Scale Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("${(scaleFactor * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = scaleFactor.toFloat(),
                    onValueChange = { scaleFactor = it.toDouble() },
                    valueRange = 0.3f..2.0f,
                    steps = 17
                )

                // Page Ranges
                KromiumInput(
                    value = pageRanges,
                    onValueChange = { pageRanges = it },
                    placeholder = "Page Ranges (e.g. 1-3, 5)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section: Action Button
        Button(
            onClick = {
                isPrinting = true
                printResult = "Generating PDF..."
                val file = File(targetPath)
                file.parentFile?.mkdirs()

                val resolvedPaperSize = paperSizes.first { it.first == selectedPaperSize }.second
                val resolvedMargins = marginOptions.first { it.first == selectedMargin }.second

                val settings = KromiumPdfSettings(
                    landscape = isLandscape,
                    printBackground = printBackground,
                    scale = scaleFactor,
                    paperSize = resolvedPaperSize,
                    margins = resolvedMargins,
                    pageRanges = pageRanges.trim(),
                    createDirectories = true
                )

                browser.view.printToPdf(targetPath, settings).thenAccept { success ->
                    isPrinting = false
                    if (success && file.exists()) {
                        printResult = "PDF successfully generated at: ${file.absolutePath}"
                        generatedFile = file
                    } else {
                        printResult = "PDF generation failed (engine returned false or file not written)."
                    }
                }.exceptionally { ex ->
                    isPrinting = false
                    printResult = "PDF generation error: ${ex.message}"
                    null
                }
            },
            enabled = !isPrinting && targetPath.isNotBlank(),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            if (isPrinting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Rendering Vector PDF...", fontSize = 12.sp)
            } else {
                Text("Export Page to PDF", fontSize = 12.sp)
            }
        }
    }
}
