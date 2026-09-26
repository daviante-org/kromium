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
import org.daviante.kromium.sample.ui.KromiumInput

@Composable
fun CookieStorageFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    var cookiesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingCookies by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // New cookie form state
    var cookieName by remember { mutableStateOf("") }
    var cookieValue by remember { mutableStateOf("") }
    var cookieDomain by remember { mutableStateOf("") }
    var cookiePath by remember { mutableStateOf("/") }
    var isSecure by remember { mutableStateOf(false) }
    var isHttpOnly by remember { mutableStateOf(false) }

    fun refreshCookies() {
        isLoadingCookies = true
        browser.storage.getCookiesAsync().thenAccept { map ->
            cookiesMap = map
            isLoadingCookies = false
            statusMessage = "Loaded ${map.size} cookies from session."
        }.exceptionally { ex ->
            isLoadingCookies = false
            statusMessage = "Error fetching cookies: ${ex.message}"
            null
        }
    }

    LaunchedEffect(Unit) {
        refreshCookies()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cookie & Storage Engine",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Inspect and mutate HTTP cookies, session storage, and origin-scoped localStorage directly through the Kromium Storage facade.",
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

        // Section: Storage Purge Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Storage Purge Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val success = browser.storage.clearCookies()
                            statusMessage = if (success) "Cleared all session cookies." else "Failed to clear cookies."
                            refreshCookies()
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Clear Cookies", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            browser.storage.clearWebStorageAsync().thenAccept { ok ->
                                statusMessage = if (ok) "Origin localStorage and sessionStorage purged." else "Storage clear failed."
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Clear Storage", fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            browser.storage.clearBrowsingDataAsync(clearCookies = true, clearStorage = true).thenAccept { ok ->
                                statusMessage = if (ok) "Purged both cookies and origin storage." else "Data purge failed."
                                refreshCookies()
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Purge All", fontSize = 10.sp)
                    }
                }
            }
        }

        // Section: Inject / Set Cookie
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Inject New Cookie",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KromiumInput(
                        value = cookieName,
                        onValueChange = { cookieName = it },
                        placeholder = "Name (e.g. token)",
                        modifier = Modifier.weight(1f)
                    )
                    KromiumInput(
                        value = cookieValue,
                        onValueChange = { cookieValue = it },
                        placeholder = "Value (e.g. abc123)",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KromiumInput(
                        value = cookieDomain,
                        onValueChange = { cookieDomain = it },
                        placeholder = "Domain (optional)",
                        modifier = Modifier.weight(1f)
                    )
                    KromiumInput(
                        value = cookiePath,
                        onValueChange = { cookiePath = it },
                        placeholder = "Path (default /)",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSecure,
                                onCheckedChange = { isSecure = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Secure", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isHttpOnly,
                                onCheckedChange = { isHttpOnly = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("HttpOnly", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (cookieName.isNotBlank()) {
                                val success = browser.storage.setCookie(
                                    name = cookieName.trim(),
                                    value = cookieValue,
                                    domain = cookieDomain.trim().takeIf { it.isNotBlank() },
                                    path = cookiePath.trim().ifEmpty { "/" },
                                    isSecure = isSecure,
                                    isHttpOnly = isHttpOnly
                                )
                                statusMessage = if (success) "Cookie '${cookieName.trim()}' set." else "Failed to set cookie."
                                if (success) {
                                    cookieName = ""
                                    cookieValue = ""
                                    refreshCookies()
                                }
                            }
                        },
                        enabled = cookieName.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Set Cookie", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section: Live Cookies List
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
                    Text(
                        text = "Current Page Cookies (${cookiesMap.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { refreshCookies() },
                        enabled = !isLoadingCookies,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        if (isLoadingCookies) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Refresh", fontSize = 10.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp)
                ) {
                    if (cookiesMap.isEmpty()) {
                        Text(
                            text = if (isLoadingCookies) "Loading cookies..." else "No cookies found for current origin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            cookiesMap.forEach { (name, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = value,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("$name=$value"))
                                            statusMessage = "Copied '$name' to clipboard."
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Copy", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
