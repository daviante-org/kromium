[Documentation Hub](../README.md) / Getting Started / **Multi-Framework UI Integration**

---

# Multi-Framework UI Integration Guide

Kromium features a decoupled Off-Screen Rendering (OSR) engine that streams raw Chromium BGRA pixel buffers directly into the native graphical surface of your chosen UI toolkit. Because rendering is off-screen, Kromium avoids heavyweight window handle (`HWND`, `NSView`, `X11`) punch-through issues, eliminates z-order airspace conflicts, and enables seamless alpha blending and overlays.

---

## Supported Frameworks

Kromium provides dedicated canvas modules for all major JVM desktop UI toolkits:

| Framework | Target Module | Framework Identifier | Canvas Surface |
| :--- | :--- | :--- | :--- |
| **Compose Multiplatform** | `kromium-compose` | `KromiumUiFramework.COMPOSE` | `KromiumView` Composable |
| **Swing** | `kromium-swing` | `KromiumUiFramework.SWING` | `KromiumSwingCanvas` (`JPanel`) |
| **JavaFX** | `kromium-javafx` | `KromiumUiFramework.JAVAFX` | `KromiumJavaFxCanvas` (`StackPane`) |
| **Eclipse SWT** | `kromium-swt` | `KromiumUiFramework.SWT` | `KromiumSwtCanvas` (`Canvas`) |
| **Pure AWT** | `kromium-awt` | `KromiumUiFramework.AWT` | `KromiumAwtCanvas` (`Canvas`) |

---

## 1. Compose Multiplatform Integration

The `kromium-compose` module provides the declarative `@Composable KromiumView` component, with built-in reactive navigation state collection and custom loading overlay slots.

### Gradle Setup

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-compose:0.1.0-SNAPSHOT")
}
```

### Complete Example

```kotlin
package com.example.app.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.compose.KromiumView
import org.daviante.kromium.jcef.JcefProvider

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kromium Compose") {
        var engine by remember { mutableStateOf<KromiumEngine?>(null) }
        var browser by remember { mutableStateOf<KromiumBrowser?>(null) }

        // Bootstrap Chromium asynchronously on launch
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val config = KromiumConfig.builder().build()
                val initializedEngine = JcefProvider.createEngine(config)
                initializedEngine.initialize()

                val client = initializedEngine.createClient()
                val createdBrowser = client.createBrowser(
                    url = "https://example.com",
                    framework = KromiumUiFramework.COMPOSE
                )

                engine = initializedEngine
                browser = createdBrowser
            }
        }

        // Clean shutdown on window close
        DisposableEffect(Unit) {
            onDispose {
                browser?.close()
                engine?.dispose()
            }
        }

        val currentBrowser = browser
        if (currentBrowser != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navState by currentBrowser.navigation.navigationState.collectAsState()

                    Button(
                        onClick = { currentBrowser.navigation.goBack() },
                        enabled = navState.canGoBack
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = { currentBrowser.navigation.goForward() },
                        enabled = navState.canGoForward
                    ) {
                        Text("Forward")
                    }

                    Button(onClick = { currentBrowser.navigation.reload() }) {
                        Text("Reload")
                    }

                    Text(
                        text = navState.url,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        maxLines = 1
                    )
                }

                // Browser View with Custom Loading Overlay Slot
                KromiumView(
                    browser = currentBrowser,
                    modifier = Modifier.fillMaxSize(),
                    loadingContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                )
            }
        } else {
            // Placeholder while native engine is downloading/bootstrapping
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Initializing Chromium Engine...")
            }
        }
    }
}
```

---

## 2. Swing Desktop Integration

The `kromium-swing` module provides `KromiumSwingCanvas`, a double-buffered `JPanel` that draws Chromium frames directly via Java2D.

### Gradle Setup

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-swing:0.1.0-SNAPSHOT")
}
```

### Complete Example

```kotlin
package com.example.app.swing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Kromium Swing Integration")
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.setSize(1280, 800)
        frame.minimumSize = Dimension(800, 600)
        frame.setLocationRelativeTo(null)

        val loadingLabel = JLabel("Bootstrapping Chromium Engine...", SwingConstants.CENTER)
        frame.contentPane.add(loadingLabel, BorderLayout.CENTER)
        frame.isVisible = true

        var engine: KromiumEngine? = null
        var browser: KromiumBrowser? = null

        // Initialize Chromium off the Event Dispatch Thread (EDT)
        CoroutineScope(Dispatchers.IO).launch {
            val config = KromiumConfig.builder().build()
            val initializedEngine = JcefProvider.createEngine(config)
            initializedEngine.initialize()

            val client = initializedEngine.createClient()
            val createdBrowser = client.createBrowser(
                url = "https://example.com",
                framework = KromiumUiFramework.SWING
            )

            engine = initializedEngine
            browser = createdBrowser

            // Unwrap the Swing Component from the browser's render surface
            val swingComponent = createdBrowser.view.surface.unwrap(Component::class)

            SwingUtilities.invokeLater {
                frame.contentPane.remove(loadingLabel)

                // Toolbar
                val toolbar = JToolBar()
                val backBtn = JButton("Back").apply {
                    addActionListener { createdBrowser.navigation.goBack() }
                }
                val reloadBtn = JButton("Reload").apply {
                    addActionListener { createdBrowser.navigation.reload() }
                }
                toolbar.add(backBtn)
                toolbar.add(reloadBtn)

                frame.contentPane.add(toolbar, BorderLayout.NORTH)
                frame.contentPane.add(swingComponent, BorderLayout.CENTER)
                frame.revalidate()
                frame.repaint()
            }
        }

        // Clean teardown on window closing
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                browser?.close()
                engine?.dispose()
                frame.dispose()
                System.exit(0)
            }
        })
    }
}
```

---

## 3. JavaFX Desktop Integration

The `kromium-javafx` module provides `KromiumJavaFxCanvas`, a native JavaFX `StackPane` that leverages JavaFX `PixelBuffer` and Prism GPU hardware acceleration with **zero Swing/AWT bridge overhead**.

### Gradle Setup

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-javafx:0.1.0-SNAPSHOT")
}
```

### Complete Example

```kotlin
package com.example.app.javafx

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider

class KromiumJavaFxApp : Application() {
    private var engine: KromiumEngine? = null
    private var browser: KromiumBrowser? = null

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val scene = Scene(root, 1280.0, 800.0)
        primaryStage.title = "Kromium JavaFX Integration"
        primaryStage.scene = scene
        primaryStage.show()

        // Bootstrap Chromium in the background
        CoroutineScope(Dispatchers.IO).launch {
            val config = KromiumConfig.builder().build()
            val initializedEngine = JcefProvider.createEngine(config)
            initializedEngine.initialize()

            val client = initializedEngine.createClient()
            val createdBrowser = client.createBrowser(
                url = "https://example.com",
                framework = KromiumUiFramework.JAVAFX
            )

            engine = initializedEngine
            browser = createdBrowser

            // Unwrap the native JavaFX Node (StackPane)
            val fxNode = createdBrowser.view.surface.unwrap(Node::class)

            Platform.runLater {
                val backBtn = Button("Back").apply {
                    setOnAction { createdBrowser.navigation.goBack() }
                }
                val reloadBtn = Button("Reload").apply {
                    setOnAction { createdBrowser.navigation.reload() }
                }
                val toolbar = ToolBar(backBtn, reloadBtn)

                root.top = toolbar
                root.center = fxNode
            }
        }

        // Clean teardown on window close
        primaryStage.setOnCloseRequest {
            browser?.close()
            engine?.dispose()
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(KromiumJavaFxApp::class.java, *args)
}
```

---

## 4. Eclipse SWT Integration

The `kromium-swt` module provides `KromiumSwtCanvas`, drawing Chromium pixel buffers directly via SWT GC, eliminating `SWT_AWT` bridge deadlocks.

> **macOS Note:** On macOS, applications running SWT must pass the `-XstartOnFirstThread` JVM argument.

### Gradle Setup

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-swt:0.1.0-SNAPSHOT")
}
```

### Complete Example

```kotlin
package com.example.app.swt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import org.daviante.kromium.swt.KromiumSwtCanvas
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

fun main() {
    val display = Display.getDefault()
    val shell = Shell(display)
    shell.text = "Kromium SWT Integration"
    shell.setSize(1280, 800)
    shell.layout = FillLayout()

    // 1. Create native SWT canvas attached to the Shell
    val canvas = KromiumSwtCanvas(shell, SWT.NO_BACKGROUND or SWT.DOUBLE_BUFFERED)

    // 2. Initialize engine in the background
    CoroutineScope(Dispatchers.IO).launch {
        val config = KromiumConfig.builder().build()
        val engine = JcefProvider.createEngine(config)
        engine.initialize()

        val client = engine.createClient()
        val clientConfig = KromiumClientConfig.builder()
            .framework(KromiumUiFramework.SWT)
            .build()

        val browser = client.createBrowser("https://example.com", clientConfig)

        display.asyncExec {
            // Attach the browser to the pre-created SWT canvas
            canvas.attach(browser)
            shell.layout(true, true)
        }

        // Clean teardown when shell is disposed
        display.asyncExec {
            shell.addListener(SWT.Dispose) {
                browser.close()
                engine.dispose()
            }
        }
    }

    shell.open()
    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    display.dispose()
}
```

---

## 5. Pure AWT Integration

The `kromium-awt` module provides `KromiumAwtCanvas` (`java.awt.Canvas`), ideal for lightweight embedded AWT applications with **zero `javax.swing.*` dependencies**.

### Gradle Setup

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-awt:0.1.0-SNAPSHOT")
}
```

### Complete Example

```kotlin
package com.example.app.awt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main() {
    val frame = Frame("Kromium Pure AWT Integration")
    frame.setSize(1280, 800)
    frame.layout = BorderLayout()
    frame.isVisible = true

    var engine: KromiumEngine? = null
    var browser: KromiumBrowser? = null

    CoroutineScope(Dispatchers.IO).launch {
        val config = KromiumConfig.builder().build()
        val initializedEngine = JcefProvider.createEngine(config)
        initializedEngine.initialize()

        val client = initializedEngine.createClient()
        val createdBrowser = client.createBrowser(
            url = "https://example.com",
            framework = KromiumUiFramework.AWT
        )

        engine = initializedEngine
        browser = createdBrowser

        // Unwrap the native java.awt.Canvas
        val awtCanvas = createdBrowser.view.surface.unwrap(Canvas::class)

        frame.add(awtCanvas, BorderLayout.CENTER)
        frame.validate()
        frame.repaint()
    }

    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            browser?.close()
            engine?.dispose()
            frame.dispose()
            System.exit(0)
        }
    })
}
```

---

## Navigation

- [← Previous: 5-Minute Quickstart Guide](quickstart.md)
- [Home: Documentation Hub](../README.md)
- [Next: Navigation & Reactive State →](../api/navigation.md)
