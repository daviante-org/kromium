package org.daviante.kromium.sample.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.api.download.KromiumDownloadListener

@Composable
fun DownloadsFeature(
    browser: KromiumBrowser,
    client: KromiumClient,
    modifier: Modifier = Modifier
) {
    var downloadItems by remember { mutableStateOf<Map<Int, KromiumDownloadItem>>(emptyMap()) }

    // Register download listener on the client session
    DisposableEffect(client) {
        val originalListener = client.downloadListener
        client.downloadListener = object : KromiumDownloadListener {
            override fun onDownloadUpdated(item: KromiumDownloadItem) {
                downloadItems = downloadItems + (item.id to item)
                originalListener?.onDownloadUpdated(item)
            }
        }
        onDispose {
            client.downloadListener = originalListener
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Download Manager",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Track download progress, speed, pause/resume, and cancellation in real time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Trigger sample downloads
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    browser.navigation.loadUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("Sample PDF", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    browser.navigation.loadUrl("https://proof.ovh.net/files/1Mb.dat")
                },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("1MB Test File", fontSize = 11.sp)
            }
        }

        // Target Folder Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("Folder: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = client.downloadDirectory.absolutePath,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Text(
            text = "Downloads (${downloadItems.size}):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (downloadItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active or recent downloads.\nClick one of the buttons above to test.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(downloadItems.values.toList().reversed()) { item ->
                    DownloadItemCard(item = item, browser = browser)
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: KromiumDownloadItem,
    browser: KromiumBrowser
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.suggestedFileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                DownloadStatusBadge(item)
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { item.percentComplete / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary
            )

            // Details: Bytes & Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val receivedMb = item.receivedBytes.toDouble() / (1024 * 1024)
                val totalMb = if (item.totalBytes > 0) item.totalBytes.toDouble() / (1024 * 1024) else 0.0
                val speedKb = item.speed.toDouble() / 1024

                Text(
                    text = "%.2f MB / %.2f MB (%d%%)".format(receivedMb, totalMb, item.percentComplete),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.isInProgress && !item.isPaused) {
                    Text(
                        text = "%.1f KB/s".format(speedKb),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action controls: Pause, Resume, Cancel
            if (item.isInProgress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isPaused) {
                        OutlinedButton(
                            onClick = { browser.downloads.resumeDownload(item.id) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Resume", fontSize = 10.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { browser.downloads.pauseDownload(item.id) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Pause", fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = { browser.downloads.cancelDownload(item.id) },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Cancel", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusBadge(item: KromiumDownloadItem) {
    val label = when {
        item.isComplete -> "DONE"
        item.isCanceled -> "CANCELLED"
        item.isPaused -> "PAUSED"
        item.isInProgress -> "ACTIVE"
        else -> "PENDING"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
