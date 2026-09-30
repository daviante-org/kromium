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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.daviante.kromium.api.core.KromiumBrowser
import kotlinx.coroutines.launch

@Composable
fun DomExtractionFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var extractedContent by remember { mutableStateOf("") }
    var extractionMeta by remember { mutableStateOf("No extraction performed yet.") }
    var isExtracting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "DOM & Content Extraction",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Asynchronously extracts DOM trees, text, and metadata from the live Chromium frame without blocking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Trigger Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isExtracting = true
                        val start = System.currentTimeMillis()
                        val html = browser.jsBridge.getHtml()
                        val elapsed = System.currentTimeMillis() - start
                        extractedContent = html
                        extractionMeta = "outerHTML: ${html.length} chars in ${elapsed}ms"
                        isExtracting = false
                    }
                },
                enabled = !isExtracting,
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("HTML", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isExtracting = true
                        val start = System.currentTimeMillis()
                        val text = browser.jsBridge.getText()
                        val elapsed = System.currentTimeMillis() - start
                        extractedContent = text
                        extractionMeta = "body.innerText: ${text.length} chars in ${elapsed}ms"
                        isExtracting = false
                    }
                },
                enabled = !isExtracting,
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("Text", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isExtracting = true
                        val start = System.currentTimeMillis()
                        val title = browser.jsBridge.evaluateJavaScript("document.title") ?: ""
                        val elapsed = System.currentTimeMillis() - start
                        extractedContent = title
                        extractionMeta = "title in ${elapsed}ms"
                        isExtracting = false
                    }
                },
                enabled = !isExtracting,
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("Title", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isExtracting = true
                        val start = System.currentTimeMillis()
                        val favicon = browser.jsBridge.getFaviconUrl() ?: "No favicon found"
                        val elapsed = System.currentTimeMillis() - start
                        extractedContent = favicon
                        extractionMeta = "favicon in ${elapsed}ms"
                        isExtracting = false
                    }
                },
                enabled = !isExtracting,
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("Favicon", fontSize = 11.sp)
            }
        }

        // Meta status & Copy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = extractionMeta,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (extractedContent.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(extractedContent))
                    },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Copy", fontSize = 10.sp)
                }
            }
        }

        // Content Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            if (isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (extractedContent.isBlank()) {
                Text(
                    text = "Select an extraction target above to inspect live DOM output.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = extractedContent,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
