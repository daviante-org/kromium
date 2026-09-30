package org.daviante.kromium.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.sample.features.ConsoleMessage
import org.daviante.kromium.sample.ui.KromiumSampleTheme
import org.daviante.kromium.sample.ui.SampleAppShell
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun main() = application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    // Manage Kromium engine, client, and browser lifecycle
    var engine by remember { mutableStateOf<KromiumEngine?>(null) }
    var client by remember { mutableStateOf<KromiumClient?>(null) }
    var browser by remember { mutableStateOf<KromiumBrowser?>(null) }
    var isDarkTheme by remember { mutableStateOf(true) }

    val consoleLogs = remember { mutableStateListOf<ConsoleMessage>() }

    LaunchedEffect(Unit) {
        val config = KromiumConfig.builder().build()
        val initializedEngine = JcefProvider.createEngine(config)
        initializedEngine.initialize()

        val createdClient = initializedEngine.createClient()

        // Attach global console listener
        createdClient.consoleMessageListener = KromiumConsoleMessageListener { msg ->
            val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            consoleLogs.add(0, ConsoleMessage(msg.message, msg.source, msg.line, timestamp))
            false
        }

        val createdBrowser = createdClient.createBrowser("https://example.com", KromiumUiFramework.COMPOSE)

        engine = initializedEngine
        client = createdClient
        browser = createdBrowser
    }

    val appIcon = remember {
        try {
            val stream = object {}.javaClass.getResourceAsStream("/icon.png")
            if (stream != null) {
                androidx.compose.ui.graphics.painter.BitmapPainter(androidx.compose.ui.res.loadImageBitmap(stream))
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    Window(
        onCloseRequest = {
            browser?.close()
            engine?.dispose()
            exitApplication()
        },
        state = windowState,
        title = "Kromium \u2014 Compose Desktop Sample & Testbench",
        icon = appIcon
    ) {
        // Set macOS Dock / Windows taskbar icon
        LaunchedEffect(Unit) {
            try {
                if (java.awt.Taskbar.isTaskbarSupported()) {
                    val taskbar = java.awt.Taskbar.getTaskbar()
                    if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                        val iconStream = object {}.javaClass.getResourceAsStream("/icon.png")
                        if (iconStream != null) {
                            val iconImage = javax.imageio.ImageIO.read(iconStream)
                            if (iconImage != null) {
                                taskbar.iconImage = iconImage
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        KromiumSampleTheme(darkTheme = isDarkTheme) {
            val activeBrowser = browser
            val activeClient = client

            if (activeBrowser != null && activeClient != null) {
                SampleAppShell(
                    browser = activeBrowser,
                    client = activeClient,
                    consoleLogs = consoleLogs,
                    onClearConsole = { consoleLogs.clear() },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Bootstrapping Chromium OSR Engine...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
