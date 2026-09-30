package org.daviante.kromium.sample.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.network.KromiumAssetFilter
import org.daviante.kromium.api.network.KromiumResourceType
import org.daviante.kromium.sample.ui.KromiumInput

@Composable
fun SecurityNetworkFeature(
    browser: KromiumBrowser,
    client: KromiumClient,
    modifier: Modifier = Modifier
) {
    var blockImages by remember { mutableStateOf(false) }
    var blockMedia by remember { mutableStateOf(false) }
    var blockFonts by remember { mutableStateOf(false) }

    var hostLockEnabled by remember { mutableStateOf(browser.security.hostLock != null) }
    var allowedDomainsText by remember { mutableStateOf(browser.security.hostLock?.joinToString(", ") ?: "wikipedia.org, example.com") }
    var restrictSubresources by remember { mutableStateOf(browser.security.hostLockSubresources) }
    var restrictSubframes by remember { mutableStateOf(browser.security.hostLockSubframes) }

    var doNotTrack by remember { mutableStateOf(client.doNotTrack) }
    var rememberPermissions by remember { mutableStateOf(client.rememberPermissions) }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Security & Network Policies",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Enforce domain-level host locking, restrict subresources, block bandwidth-heavy media and web fonts, and control privacy headers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        statusMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Section: Asset Blocking
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bandwidth & Resource Filtering",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Images", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Suppresses image downloads (png, jpg, webp, svg)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = blockImages,
                        onCheckedChange = { blockImages = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Audio & Video", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Suppresses HTML5 video streams and audio media", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = blockMedia,
                        onCheckedChange = { blockMedia = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Web Fonts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Blocks woff, woff2, and ttf font network requests", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = blockFonts,
                        onCheckedChange = { blockFonts = it }
                    )
                }

                Button(
                    onClick = {
                        val blockedTypes = mutableSetOf<KromiumResourceType>()
                        if (blockImages) blockedTypes.add(KromiumResourceType.IMAGE)
                        if (blockMedia) blockedTypes.add(KromiumResourceType.MEDIA)
                        if (blockFonts) blockedTypes.add(KromiumResourceType.FONT_RESOURCE)
                        
                        browser.assets.filter = if (blockedTypes.isEmpty()) {
                            null
                        } else {
                            KromiumAssetFilter(blockedResourceTypes = blockedTypes)
                        }
                        statusMessage = "Asset filter rules updated: images=$blockImages, media=$blockMedia, fonts=$blockFonts"
                    },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.align(Alignment.End).height(30.dp)
                ) {
                    Text("Apply Asset Filters", fontSize = 11.sp)
                }
            }
        }

        // Section: Host Lock
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Domain Host Lock (Sandbox)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Reject any top-level navigation outside whitelist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hostLockEnabled,
                        onCheckedChange = { hostLockEnabled = it }
                    )
                }

                if (hostLockEnabled) {
                    KromiumInput(
                        value = allowedDomainsText,
                        onValueChange = { allowedDomainsText = it },
                        placeholder = "Whitelist (e.g. example.com, wikipedia.org)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Restrict Subresources (XHR / Fetch / Scripts)", style = MaterialTheme.typography.labelSmall)
                        Checkbox(
                            checked = restrictSubresources,
                            onCheckedChange = { restrictSubresources = it },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Restrict Subframes (iFrames)", style = MaterialTheme.typography.labelSmall)
                        Checkbox(
                            checked = restrictSubframes,
                            onCheckedChange = { restrictSubframes = it },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (hostLockEnabled) {
                            val domains = allowedDomainsText.split(",")
                                .map { it.trim().lowercase() }
                                .filter { it.isNotEmpty() }
                                .toSet()
                            browser.security.hostLock = domains
                            browser.security.hostLockSubresources = restrictSubresources
                            browser.security.hostLockSubframes = restrictSubframes
                            statusMessage = "Host lock activated for: ${domains.joinToString(", ")}"
                        } else {
                            browser.security.hostLock = null
                            statusMessage = "Host lock deactivated. All domains allowed."
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.align(Alignment.End).height(30.dp)
                ) {
                    Text("Apply Host Lock", fontSize = 11.sp)
                }
            }
        }

        // Section: Privacy & Permissions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Client Privacy & Device Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Do Not Track (DNT) / Sec-GPC", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Assert DNT=1 and Global Privacy Control headers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = doNotTrack,
                        onCheckedChange = {
                            doNotTrack = it
                            client.doNotTrack = it
                            statusMessage = "Do Not Track header set to $it"
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remember Permission Decisions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Cache media/device approvals for this session", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = rememberPermissions,
                        onCheckedChange = {
                            rememberPermissions = it
                            client.rememberPermissions = it
                            statusMessage = "Remember permissions set to $it"
                        }
                    )
                }
            }
        }
    }
}
