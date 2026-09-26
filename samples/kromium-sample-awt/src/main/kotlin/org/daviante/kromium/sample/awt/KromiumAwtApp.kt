package org.daviante.kromium.sample.awt

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.sample.awt.model.ConsoleEntry
import org.daviante.kromium.sample.awt.ui.AwtAppShell
import org.daviante.kromium.sample.awt.ui.AwtTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.Taskbar
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.awt.EventQueue
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    System.setProperty("apple.awt.application.name", "Kromium AWT")

    val frame = Frame("Kromium \u2014 AWT Desktop Browser")
    frame.layout = BorderLayout()
    frame.setSize(1400, 900)
    frame.minimumSize = Dimension(900, 600)
    frame.setLocationRelativeTo(null)
    frame.background = AwtTheme.background

    // Set application icon for window & macOS/Windows Taskbar
    try {
        val iconStream = object {}.javaClass.getResourceAsStream("/icon.png")
        if (iconStream != null) {
            val iconImage = ImageIO.read(iconStream)
            if (iconImage != null) {
                frame.iconImage = iconImage
                if (Taskbar.isTaskbarSupported()) {
                    val taskbar = Taskbar.getTaskbar()
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.iconImage = iconImage
                    }
                }
            }
        }
    } catch (_: Throwable) {}

    val consoleLogs = CopyOnWriteArrayList<ConsoleEntry>()
    var engine: KromiumEngine? = null
    var client: KromiumClient? = null
    var browser: KromiumBrowser? = null

    val appShell = AwtAppShell(
        frame = frame,
        onClearConsole = { consoleLogs.clear() }
    )
    frame.add(appShell, BorderLayout.CENTER)

    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            browser?.close()
            engine?.dispose()
            frame.dispose()
            exitProcess(0)
        }
    })

    frame.isVisible = true

    // Initialize Kromium off the main thread
    CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = KromiumConfig.builder().build()
                val initializedEngine = JcefProvider.createEngine(config)
                initializedEngine.initialize()

            val clientConfig = KromiumClientConfig.builder()
                .framework(KromiumUiFramework.AWT)
                .build()
            val createdClient = initializedEngine.createClient(clientConfig)

            createdClient.consoleMessageListener = KromiumConsoleMessageListener { msg ->
                val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val entry = ConsoleEntry(
                    message = msg.message,
                    source = msg.source,
                    line = msg.line,
                    timestamp = timestamp,
                    level = msg.level.name
                )
                consoleLogs.add(0, entry)
                EventQueue.invokeLater {
                    appShell.onConsoleMessageAdded(entry)
                }
                false
            }

            val createdBrowser = createdClient.createBrowser("https://example.com", clientConfig)

            engine = initializedEngine
            client = createdClient
            browser = createdBrowser

            EventQueue.invokeLater {
                appShell.attachBrowser(createdBrowser, createdClient)
                consoleLogs.forEach { appShell.onConsoleMessageAdded(it) }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
