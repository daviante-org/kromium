package org.daviante.kromium.sample.swt

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.sample.swt.model.ConsoleEntry
import org.daviante.kromium.sample.swt.ui.SwtAppShell
import org.daviante.kromium.sample.swt.ui.SwtTheme
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.awt.Taskbar
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    System.setProperty("apple.awt.application.name", "Kromium SWT")
    Display.setAppName("Kromium SWT")

    val display = Display()
    val shell = Shell(display)
    val theme = SwtTheme(display)
    theme.registerDispose(shell)

    shell.text = "Kromium \u2014 SWT Desktop Browser"
    shell.setSize(1400, 900)
    shell.minimumSize = org.eclipse.swt.graphics.Point(900, 600)
    shell.layout = GridLayout(1, false).apply {
        marginWidth = 0
        marginHeight = 0
        verticalSpacing = 0
    }
    shell.background = theme.background

    // Set application icon for SWT Shell & OS Taskbar / Dock
    try {
        val iconStream = object {}.javaClass.getResourceAsStream("/icon.png")
        if (iconStream != null) {
            val swtIcon = Image(display, iconStream)
            shell.image = swtIcon
            shell.addDisposeListener { swtIcon.dispose() }
        }
    } catch (_: Throwable) {}

    try {
        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                val iconStream = object {}.javaClass.getResourceAsStream("/icon.png")
                if (iconStream != null) {
                    val awtImg = ImageIO.read(iconStream)
                    if (awtImg != null) {
                        taskbar.iconImage = awtImg
                    }
                }
            }
        }
    } catch (_: Throwable) {}

    val consoleLogs = CopyOnWriteArrayList<ConsoleEntry>()
    var engine: KromiumEngine? = null
    var client: KromiumClient? = null
    var browser: KromiumBrowser? = null

    // Construct the entire AppShell with embedded AWT frame BEFORE opening the shell
    val appShell = SwtAppShell(
        shell = shell,
        theme = theme,
        onClearConsole = { consoleLogs.clear() }
    )

    // Open Shell with fully initialized Win32 HWND hierarchy
    shell.open()

    // Initialize Kromium engine in the background
    CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = KromiumConfig.builder().build()
                val initializedEngine = JcefProvider.createEngine(config)
                initializedEngine.initialize()

            val clientConfig = KromiumClientConfig.builder()
                .framework(KromiumUiFramework.SWT)
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
                appShell.onConsoleMessageAdded(entry)
                false
            }

            val createdBrowser = createdClient.createBrowser("https://example.com", clientConfig)

            engine = initializedEngine
            client = createdClient
            browser = createdBrowser

            display.asyncExec {
                if (!shell.isDisposed) {
                    appShell.attachBrowser(createdBrowser, createdClient)
                    consoleLogs.forEach { appShell.onConsoleMessageAdded(it) }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // Main SWT Event Dispatch Loop
    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }

    // Clean shutdown after shell is closed
    display.dispose()
    try {
        browser?.close()
        engine?.dispose()
    } catch (_: Throwable) {}
    exitProcess(0)
}
