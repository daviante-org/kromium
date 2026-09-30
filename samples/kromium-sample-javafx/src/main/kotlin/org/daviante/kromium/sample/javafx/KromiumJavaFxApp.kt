package org.daviante.kromium.sample.javafx

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.sample.javafx.model.ConsoleMessage
import org.daviante.kromium.sample.javafx.ui.JavaFxAppShell
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.awt.Taskbar
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class KromiumJavaFxApp : Application() {

    private var engine: KromiumEngine? = null
    private var client: KromiumClient? = null
    private var browser: KromiumBrowser? = null

    private val consoleLogs = FXCollections.observableArrayList<ConsoleMessage>()

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        root.style = "-fx-background-color: #0e1117;"

        // Bootstrap Loading View
        val loadingBox = VBox(16.0).apply {
            alignment = Pos.CENTER
            style = "-fx-background-color: #0e1117;"
        }

        val brandBadge = Label("KROMIUM").apply {
            styleClass.add("kromium-badge")
            style = "-fx-font-size: 14px; -fx-padding: 6 16 6 16;"
        }

        val spinner = ProgressIndicator().apply {
            prefWidth = 40.0
            prefHeight = 40.0
        }

        val loadingLabel = Label("Bootstrapping Chromium OSR Engine...").apply {
            style = "-fx-text-fill: #8b949e; -fx-font-size: 13px;"
        }

        loadingBox.children.addAll(brandBadge, spinner, loadingLabel)
        root.center = loadingBox

        val scene = Scene(root, 1350.0, 850.0)

        // Load CSS stylesheet
        val cssResource = javaClass.getResource("/styles.css")
        if (cssResource != null) {
            scene.stylesheets.add(cssResource.toExternalForm())
        }

        primaryStage.title = "Kromium \u2014 JavaFX Desktop Browser & Workbench"
        primaryStage.minWidth = 900.0
        primaryStage.minHeight = 600.0
        primaryStage.scene = scene

        // Set Application Icons
        try {
            val iconStream = javaClass.getResourceAsStream("/icon.png")
            if (iconStream != null) {
                primaryStage.icons.add(Image(iconStream))
            }
        } catch (_: Throwable) {}

        try {
            if (Taskbar.isTaskbarSupported()) {
                val taskbar = Taskbar.getTaskbar()
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    val awtIconStream = javaClass.getResourceAsStream("/icon.png")
                    if (awtIconStream != null) {
                        val awtImg = ImageIO.read(awtIconStream)
                        if (awtImg != null) {
                            taskbar.iconImage = awtImg
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        // Handle Clean Shutdown
        primaryStage.setOnCloseRequest {
            browser?.close()
            engine?.dispose()
            Platform.exit()
            exitProcess(0)
        }

        primaryStage.show()

        // Off-thread initialization of the Chromium OSR Engine
        CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = KromiumConfig.builder().build()
                    val initializedEngine = JcefProvider.createEngine(config)
                    initializedEngine.initialize()

                val clientConfig = KromiumClientConfig.builder()
                    .framework(KromiumUiFramework.JAVAFX)
                    .build()
                val createdClient = initializedEngine.createClient(clientConfig)

                // Attach real-time console listener
                createdClient.consoleMessageListener = KromiumConsoleMessageListener { msg ->
                    val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    Platform.runLater {
                        consoleLogs.add(0, ConsoleMessage(msg.message, msg.source, msg.line, timestamp))
                    }
                    false
                }

                val createdBrowser = createdClient.createBrowser("https://example.com", clientConfig)

                engine = initializedEngine
                client = createdClient
                browser = createdBrowser

                Platform.runLater {
                    val appShell = JavaFxAppShell(
                        browser = createdBrowser,
                        client = createdClient,
                        consoleLogs = consoleLogs,
                        onClearConsole = { consoleLogs.clear() }
                    )
                    root.center = appShell
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                Platform.runLater {
                    loadingLabel.text = "Failed to initialize Chromium engine: ${t.message}"
                    loadingLabel.style = "-fx-text-fill: #f85149; -fx-font-size: 13px;"
                }
            }
        }
    }
}
