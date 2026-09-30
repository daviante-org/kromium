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

import org.daviante.kromium.sample.ui.KromiumInput

@Composable
fun NavigationFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    val navState by browser.navigation.navigationState.collectAsState()
    var inputUrl by remember { mutableStateOf("https://example.com") }
    var isInputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(navState.url) {
        if (!isInputFocused && navState.url.isNotBlank()) {
            inputUrl = navState.url
        }
    }

    val presetUrls = listOf(
        "https://example.com" to "Example",
        "https://wikipedia.org" to "Wikipedia",
        "https://github.com" to "GitHub",
        "https://browserleaks.com" to "BrowserLeaks"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Navigation & Session History",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Fine-grained navigation control: navigate backwards/forwards in history, perform cache-busting hard reloads, or observe live state flows.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // URL Input & Go Button
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KromiumInput(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        placeholder = "URL...",
                        onEnter = {
                            val target = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                                "https://$inputUrl"
                            } else inputUrl
                            browser.navigation.loadUrl(target)
                        },
                        onFocusChanged = { isInputFocused = it },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val target = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                                "https://$inputUrl"
                            } else inputUrl
                            browser.navigation.loadUrl(target)
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Load", fontSize = 11.sp)
                    }
                }

                // Action Toolbar: Back, Forward, Reload, Reload Ignoring Cache, Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { browser.navigation.goBack() },
                        enabled = navState.canGoBack || browser.navigation.canGoBack(),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Back", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { browser.navigation.goForward() },
                        enabled = navState.canGoForward || browser.navigation.canGoForward(),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Forward", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { browser.navigation.reload(ignoreCache = false) },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Reload", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { browser.navigation.reload(ignoreCache = true) },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Hard Reload", fontSize = 10.sp)
                    }

                    if (navState.isLoading) {
                        Button(
                            onClick = { browser.navigation.stopLoad() },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("Stop", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Quick Preset Targets
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetUrls.forEach { (url, label) ->
                        OutlinedButton(
                            onClick = {
                                inputUrl = url
                                browser.navigation.loadUrl(url)
                            },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text(label, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Real-time Reactive NavigationState telemetry card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Live Navigation State",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StateBadge(label = "Loading", active = navState.isLoading, activeText = "ACTIVE", inactiveText = "IDLE")
                    StateBadge(label = "Can Go Back", active = navState.canGoBack || browser.navigation.canGoBack(), activeText = "YES", inactiveText = "NO")
                    StateBadge(label = "Can Go Forward", active = navState.canGoForward || browser.navigation.canGoForward(), activeText = "YES", inactiveText = "NO")
                }
            }
        }
    }
}

@Composable
private fun StateBadge(
    label: String,
    active: Boolean,
    activeText: String,
    inactiveText: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.dp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (active) activeText else inactiveText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
