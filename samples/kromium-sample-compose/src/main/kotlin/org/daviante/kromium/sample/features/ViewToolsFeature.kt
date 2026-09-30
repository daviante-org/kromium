package org.daviante.kromium.sample.features

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.daviante.kromium.api.core.KromiumBrowser
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.image.BufferedImage

@Composable
fun ViewToolsFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    var currentZoom by remember { mutableStateOf(browser.view.zoomLevel) }
    var antialiasingEnabled by remember { mutableStateOf(true) }
    var selectedInterpolation by remember { mutableStateOf("bicubic") }
    var capturedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var screenshotInfo by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val interpolationOptions = listOf("nearest_neighbor", "bilinear", "bicubic", "high")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "View & Rendering Controls",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Section: Zoom
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zoom", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "%.2f (approx %.0f%%)".format(currentZoom, (1.0 + currentZoom * 0.2) * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Slider(
                    value = currentZoom.toFloat(),
                    onValueChange = { newVal ->
                        currentZoom = newVal.toDouble()
                        browser.view.setZoom(currentZoom)
                    },
                    valueRange = -2.0f..2.0f,
                    steps = 7,
                    modifier = Modifier.height(24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(-1.0 to "80%", 0.0 to "100%", 1.0 to "120%", 2.0 to "140%").forEach { (level, label) ->
                        OutlinedButton(
                            onClick = {
                                currentZoom = level
                                browser.view.setZoom(level)
                            },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text(label, fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = {
                            currentZoom = 0.0
                            browser.view.setZoom(0.0)
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Reset", fontSize = 10.sp)
                    }
                }
            }
        }

        // Section: Anti-aliasing and Interpolation
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Quality & Interpolation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anti-Aliasing", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = antialiasingEnabled,
                        onCheckedChange = { checked ->
                            antialiasingEnabled = checked
                            browser.view.setAntialiasing(checked)
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text("Texture Interpolation:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    interpolationOptions.forEach { mode ->
                        FilterChip(
                            selected = selectedInterpolation == mode,
                            onClick = {
                                selectedInterpolation = mode
                                browser.view.setInterpolation(mode)
                            },
                            shape = RoundedCornerShape(4.dp),
                            label = { Text(mode.replace("_", " "), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(28.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Calibrate DPI", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { browser.view.resetScaleFactorToAuto() },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Calibrate", fontSize = 10.sp)
                    }
                }
            }
        }

        // Section: Screenshot Capture
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Screenshot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                    Button(
                        onClick = {
                            isCapturing = true
                            browser.view.takeScreenshotAsync().thenAccept { bImg ->
                                if (bImg != null) {
                                    val composeImg = bufferedImageToImageBitmap(bImg)
                                    capturedBitmap = composeImg
                                    screenshotInfo = "${bImg.width} \u00D7 ${bImg.height} px"
                                } else {
                                    screenshotInfo = "Capture failed (null)"
                                }
                                isCapturing = false
                            }.exceptionally { ex ->
                                screenshotInfo = "Error: ${ex.message}"
                                isCapturing = false
                                null
                            }
                        },
                        enabled = !isCapturing,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Capture", fontSize = 11.sp)
                        }
                    }
                }

                screenshotInfo?.let { info ->
                    Text(
                        text = info,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                capturedBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Captured Page Screenshot",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun bufferedImageToImageBitmap(image: BufferedImage): ImageBitmap {
    val width = image.width
    val height = image.height
    val pixels = IntArray(width * height)
    image.getRGB(0, 0, width, height, pixels, 0, width)
    
    val bytes = ByteArray(width * height * 4)
    for (i in pixels.indices) {
        val argb = pixels[i]
        val a = (argb shr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val offset = i * 4
        bytes[offset] = r.toByte()
        bytes[offset + 1] = g.toByte()
        bytes[offset + 2] = b.toByte()
        bytes[offset + 3] = a.toByte()
    }

    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    val bitmap = Bitmap().apply {
        allocPixels(info)
        installPixels(info, bytes, width * 4)
    }
    return bitmap.asComposeImageBitmap()
}
