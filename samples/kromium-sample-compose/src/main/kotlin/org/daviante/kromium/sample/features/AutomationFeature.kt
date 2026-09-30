@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class AutomationAction(val label: String) {
    WAIT_FOR("Wait"),
    CLICK("Click"),
    FILL("Fill"),
    TYPE("Type"),
    GET_TEXT("Get Text"),
    GET_ATTRIBUTE("Get Attr"),
    IS_VISIBLE("Visible?"),
    IS_CHECKED("Checked?"),
    COUNT("Count")
}

@Composable
fun AutomationFeature(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier
) {
    var selector by remember { mutableStateOf("input, button, h1, a") }
    var actionArg by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(AutomationAction.COUNT) }
    var isRunning by remember { mutableStateOf(false) }

    val logEntries = remember { mutableStateListOf<String>() }

    fun addLog(entry: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        logEntries.add(0, "[$time] $entry")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Playwright DOM Automation",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Desktop Emulation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Desktop Emulation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Injects scripts to spoof desktop screen & navigator properties.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        browser.automation.emulateDesktopEnvironment()
                        addLog("Emulation script dispatched.")
                    },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Emulate", fontSize = 11.sp)
                }
            }
        }

        // Action Builder Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Query Runner",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Action chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AutomationAction.values().forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            shape = RoundedCornerShape(4.dp),
                            label = { Text(action.label, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Selector input
                KromiumInput(
                    value = selector,
                    onValueChange = { selector = it },
                    placeholder = "CSS Selector (e.g. h1, button, a)",
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional argument input
                if (selectedAction in listOf(AutomationAction.FILL, AutomationAction.TYPE, AutomationAction.GET_ATTRIBUTE)) {
                    val argPlaceholder = when (selectedAction) {
                        AutomationAction.GET_ATTRIBUTE -> "Attribute name (e.g. href, src)"
                        AutomationAction.FILL -> "Value to fill"
                        AutomationAction.TYPE -> "Text to type"
                        else -> "Argument"
                    }
                    KromiumInput(
                        value = actionArg,
                        onValueChange = { actionArg = it },
                        placeholder = argPlaceholder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Execute Button
                Button(
                    onClick = {
                        isRunning = true
                        val targetSelector = selector.trim()
                        when (selectedAction) {
                            AutomationAction.WAIT_FOR -> {
                                browser.automation.waitForSelectorAsync(targetSelector).thenAccept { found ->
                                    addLog("waitForSelector('$targetSelector') -> $found")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("waitForSelector ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.CLICK -> {
                                browser.automation.clickAsync(targetSelector).thenAccept { clicked ->
                                    addLog("click('$targetSelector') -> $clicked")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("click ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.FILL -> {
                                browser.automation.fillAsync(targetSelector, actionArg).thenAccept { filled ->
                                    addLog("fill('$targetSelector', '$actionArg') -> $filled")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("fill ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.TYPE -> {
                                browser.automation.typeAsync(targetSelector, actionArg).thenAccept { typed ->
                                    addLog("type('$targetSelector', '$actionArg') -> $typed")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("type ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.GET_TEXT -> {
                                browser.automation.getTextContentAsync(targetSelector).thenAccept { text ->
                                    addLog("getText('$targetSelector') -> \"$text\"")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("getText ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.GET_ATTRIBUTE -> {
                                browser.automation.getAttributeAsync(targetSelector, actionArg).thenAccept { attr ->
                                    addLog("getAttr('$targetSelector', '$actionArg') -> \"$attr\"")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("getAttr ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.IS_VISIBLE -> {
                                browser.automation.isVisibleAsync(targetSelector).thenAccept { visible ->
                                    addLog("isVisible('$targetSelector') -> $visible")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("isVisible ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.IS_CHECKED -> {
                                browser.automation.isCheckedAsync(targetSelector).thenAccept { checked ->
                                    addLog("isChecked('$targetSelector') -> $checked")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("isChecked ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                            AutomationAction.COUNT -> {
                                browser.automation.countAsync(targetSelector).thenAccept { count ->
                                    addLog("count('$targetSelector') -> $count")
                                    isRunning = false
                                }.exceptionally { ex ->
                                    addLog("count ERROR: ${ex.message}")
                                    isRunning = false
                                    null
                                }
                            }
                        }
                    },
                    enabled = !isRunning && selector.isNotBlank(),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.align(Alignment.End).height(32.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Execute", fontSize = 11.sp)
                    }
                }
            }
        }

        // Live Log Output
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Log (${logEntries.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (logEntries.isNotEmpty()) {
                        TextButton(
                            onClick = { logEntries.clear() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Clear", fontSize = 10.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp)
                ) {
                    if (logEntries.isEmpty()) {
                        Text(
                            text = "No actions executed yet.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            logEntries.forEach { entry ->
                                Text(
                                    text = entry,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
