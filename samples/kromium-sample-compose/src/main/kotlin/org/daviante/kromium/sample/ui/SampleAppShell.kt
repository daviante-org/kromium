package org.daviante.kromium.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.compose.KromiumView
import org.daviante.kromium.compose.chrome.MacTrafficLightsSpacer
import org.daviante.kromium.sample.features.*
import org.daviante.kromium.sample.model.SampleFeature

@Composable
fun SampleAppShell(
    browser: KromiumBrowser,
    client: KromiumClient,
    consoleLogs: List<ConsoleMessage>,
    onClearConsole: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFeature by remember { mutableStateOf(SampleFeature.NAVIGATION) }
    var isWorkbenchVisible by remember { mutableStateOf(true) }
    val navState by browser.navigation.navigationState.collectAsState()

    var addressBarUrl by remember { mutableStateOf("https://example.com") }
    var isAddressBarFocused by remember { mutableStateOf(false) }

    LaunchedEffect(navState.url) {
        if (!isAddressBarFocused && navState.url.isNotBlank()) {
            addressBarUrl = navState.url
        }
    }

    fun navigateTo(url: String) {
        val target = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        addressBarUrl = target
        browser.navigation.loadUrl(target)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- GLOBAL COMPACT BROWSER NAVIGATION BAR ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // macOS window traffic lights
                MacTrafficLightsSpacer()

                // Branding Label
                Text(
                    text = "KROMIUM",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outline)

                // Navigation Controls: Back, Forward, Reload/Stop
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Back Button
                    OutlinedButton(
                        onClick = { browser.navigation.goBack() },
                        enabled = navState.canGoBack || browser.navigation.canGoBack(),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("◀", fontSize = 12.sp)
                    }

                    // Forward Button
                    OutlinedButton(
                        onClick = { browser.navigation.goForward() },
                        enabled = navState.canGoForward || browser.navigation.canGoForward(),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("▶", fontSize = 12.sp)
                    }

                    // Reload or Stop Button
                    if (navState.isLoading) {
                        Button(
                            onClick = { browser.navigation.stopLoad() },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("✖", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { browser.navigation.reload(ignoreCache = false) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("⟳", fontSize = 14.sp)
                        }
                    }
                }

                // Global Address Bar
                KromiumInput(
                    value = addressBarUrl,
                    onValueChange = { addressBarUrl = it },
                    placeholder = "Enter URL (e.g. example.com, wikipedia.org)...",
                    onEnter = { navigateTo(addressBarUrl) },
                    onFocusChanged = { isAddressBarFocused = it },
                    modifier = Modifier.weight(1f)
                )

                // Go Button
                Button(
                    onClick = { navigateTo(addressBarUrl) },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Go", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Zoom Controls
                OutlinedButton(
                    onClick = { browser.view.zoomLevel -= 0.5 },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("−", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { browser.view.zoomLevel = 0.0 },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("100%", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { browser.view.zoomLevel += 0.5 },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+", fontSize = 12.sp)
                }

                // DevTools Button
                OutlinedButton(
                    onClick = { browser.devTools.openDevTools() },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("🛠 DevTools", fontSize = 11.sp)
                }

                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outline)

                // Quick Presets Dropdown/Buttons
                listOf(
                    "Google" to "https://www.google.com",
                    "GitHub" to "https://github.com/daviante-org/kromium",
                    "Wikipedia" to "https://www.wikipedia.org",
                    "WebGL Aquarium" to "https://webglsamples.org/aquarium/aquarium.html",
                    "Acid3" to "http://acid3.acidtests.org/"
                ).forEach { (domain, url) ->
                    OutlinedButton(
                        onClick = { navigateTo(url) },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(domain, fontSize = 11.sp)
                    }
                }

                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outline)

                // Toggle Side Workbench Panel
                OutlinedButton(
                    onClick = { isWorkbenchVisible = !isWorkbenchVisible },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isWorkbenchVisible) "⊟ Workbench" else "⊞ Workbench", fontSize = 11.sp)
                }

                // Theme Switch (Black/White)
                OutlinedButton(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isDarkTheme) "Light" else "Dark", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // --- WORKBENCH & LIVE BROWSER WORKSPACE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 1. LEFT SIDEBAR: Feature Navigation (180.dp) - Only shown if workbench is visible
            if (isWorkbenchVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "FEATURES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        SampleFeature.values().forEach { feature ->
                            val isSelected = feature == selectedFeature
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { selectedFeature = feature }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = feature.iconTag,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = feature.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // 2. CENTER: Live Browser Surface (Responsive, takes all available space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                KromiumView(
                    browser = browser,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. RIGHT PANEL: Active Feature Controls (360.dp) - Collapsible
            if (isWorkbenchVisible) {
                VerticalDivider(color = MaterialTheme.colorScheme.outline)

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                ) {
                    when (selectedFeature) {
                        SampleFeature.NAVIGATION -> NavigationFeature(browser = browser)
                        SampleFeature.DOM_EXTRACTION -> DomExtractionFeature(browser = browser)
                        SampleFeature.DOWNLOADS -> DownloadsFeature(browser = browser, client = client)
                        SampleFeature.VIEW_TOOLS -> ViewToolsFeature(browser = browser)
                        SampleFeature.AUTOMATION -> AutomationFeature(browser = browser)
                        SampleFeature.COOKIE_STORAGE -> CookieStorageFeature(browser = browser)
                        SampleFeature.PDF_PRINT -> PdfPrintFeature(browser = browser)
                        SampleFeature.DEVTOOLS -> DevToolsFeature(
                            browser = browser,
                            consoleLogs = consoleLogs,
                            onClearConsole = onClearConsole
                        )
                        SampleFeature.SECURITY_NETWORK -> SecurityNetworkFeature(browser = browser, client = client)
                    }
                }
            }
        }
    }
}
