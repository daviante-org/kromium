package org.daviante.kromium.sample.swing

import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.sample.swing.model.ConsoleMessage
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.SwingAppShell
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Kromium \u2014 Swing Desktop Sample & Testbench")
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.setSize(1400, 900)
        frame.minimumSize = Dimension(900, 600)
        frame.setLocationRelativeTo(null)

        // Set application icon for window & macOS/Windows Taskbar
        try {
            val iconStream = object {}.javaClass.getResourceAsStream("/icon.png")
            if (iconStream != null) {
                val iconImage = javax.imageio.ImageIO.read(iconStream)
                if (iconImage != null) {
                    frame.iconImage = iconImage
                    if (java.awt.Taskbar.isTaskbarSupported()) {
                        val taskbar = java.awt.Taskbar.getTaskbar()
                        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                            taskbar.iconImage = iconImage
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        // Loading placeholder panel while Chromium engine bootstraps
        val loadingPanel = JPanel()
        loadingPanel.layout = BoxLayout(loadingPanel, BoxLayout.Y_AXIS)
        loadingPanel.background = SwingTheme.background

        val spinner = JProgressBar()
        spinner.isIndeterminate = true
        spinner.preferredSize = Dimension(200, 4)
        spinner.maximumSize = Dimension(200, 4)
        spinner.background = SwingTheme.surfaceVariant
        spinner.foreground = SwingTheme.primary

        val statusText = KromiumLabel("Bootstrapping Chromium OSR Engine...", isBold = false, fontSize = 12f)
        statusText.alignmentX = 0.5f
        spinner.alignmentX = 0.5f

        loadingPanel.add(Box.createVerticalGlue())
        loadingPanel.add(spinner)
        loadingPanel.add(Box.createVerticalStrut(12))
        loadingPanel.add(statusText)
        loadingPanel.add(Box.createVerticalGlue())

        frame.contentPane.add(loadingPanel, BorderLayout.CENTER)
        frame.isVisible = true

        val consoleLogs = CopyOnWriteArrayList<ConsoleMessage>()
        var appShell: SwingAppShell? = null
        var engine: KromiumEngine? = null
        var browser: KromiumBrowser? = null

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                browser?.close()
                engine?.dispose()
                frame.dispose()
                exitProcess(0)
            }
        })

        // Initialize Chromium off the EDT to keep UI responsive
        CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = KromiumConfig.builder().build()
                    val initializedEngine = JcefProvider.createEngine(config)
                    initializedEngine.initialize()

                val createdClient = initializedEngine.createClient()

                // Attach global console listener
                createdClient.consoleMessageListener = KromiumConsoleMessageListener { msg ->
                    val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    consoleLogs.add(0, ConsoleMessage(msg.message, msg.source, msg.line, timestamp))
                    SwingUtilities.invokeLater {
                        appShell?.onConsoleMessageAdded()
                    }
                    false
                }

                val createdBrowser = createdClient.createBrowser("https://example.com", KromiumUiFramework.SWING)

                engine = initializedEngine
                browser = createdBrowser

                SwingUtilities.invokeLater {
                    frame.contentPane.remove(loadingPanel)
                    val shell = SwingAppShell(
                        browser = createdBrowser,
                        client = createdClient,
                        consoleLogs = consoleLogs,
                        onClearConsole = { consoleLogs.clear() }
                    )
                    appShell = shell
                    frame.contentPane.add(shell, BorderLayout.CENTER)
                    frame.revalidate()
                    frame.repaint()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                SwingUtilities.invokeLater {
                    statusText.text = "Failed to initialize Chromium: ${t.message}"
                }
            }
        }
    }
}
