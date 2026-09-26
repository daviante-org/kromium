# Kromium

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.7.3-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/JDK-17%2B-orange.svg)](https://adoptium.net)

**Kromium** is a modern, high-performance, developer-first Chromium browser engine for Kotlin and Java desktop applications. Built on top of a modular Service Provider Interface (SPI) and Chromium Embedded Framework (CEF), Kromium delivers true off-screen rendering (OSR), modern coroutine-native APIs, and seamless embedding across JVM desktop UI toolkits.

---

## Key Features

- **Provider-Agnostic Public API:** Clean, decoupled facade layer ([`kromium-api`](kromium-api)) with zero internal native CEF types exposed.
- **Universal Desktop UI Embedding:** Native canvas integrations with zero heavyweight window handle punch-through or airspace conflicts:
  - **Compose Multiplatform:** `@Composable KromiumView` with reactive loading overlays.
  - **Swing:** `KromiumSwingCanvas` (`JPanel`) with double-buffered Java2D rendering.
  - **JavaFX:** `KromiumJavaFxCanvas` (`StackPane`) powered by Prism GPU `PixelBuffer`.
  - **Eclipse SWT:** `KromiumSwtCanvas` (`Canvas`) with direct GC drawing.
  - **Pure AWT:** `KromiumAwtCanvas` (`Canvas`) for lightweight AWT containers.
- **Reactive Coroutine Streams:** Full observability with `StateFlow<KromiumNavigationState>` tracking real-time loading stages (`DOM_CONTENT_LOADED`, `LOADED`, `NETWORK_IDLE`), progress, and page metadata.
- **Enterprise Web Automation:** Built-in Playwright-style automation (`browser.automation`) supporting React/Vue controlled inputs, progressive human typing, and in-V8 `MutationObserver` selector waiting without IPC polling lag.
- **Enterprise Networking & Proxies:** Native support for `Http`, `Socks5` (with remote DNS resolution), `MultiProtocol` routing, domain bypass lists, and dynamic runtime proxy switching without engine restarts.
- **Developer Tools & Diagnostics:** Integrated Chrome DevTools inspector with inspect-point positioning, and typed browser console interception (`DEBUG`, `INFO`, `WARNING`, `ERROR`).

---

## Architecture Overview

```
                      +---------------------------------------+
                      |       Your Desktop Application        |
                      +---------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                   kromium-api                                     |
|  KromiumBrowser (Facade):                                                         |
|    * navigation:    loadUrl(), reload(), StateFlow<KromiumNavigationState>        |
|    * automation:    fill(), type(), click(), waitForSelector(), queryText()       |
|    * devTools:      openDevTools(), closeDevTools(), consoleMessageListener       |
|    * view:          surface, zoom, viewport, visibility                          |
|    * ui:            jsDialogListener, contextMenuListener                         |
|    * network:       authListener, customSchemes, proxies                          |
|    * security:      certificateErrors, permissionDecisions                        |
|    * downloads:     downloadListener, pause/resume/cancel                         |
+-----------------------------------------------------------------------------------+
      |                       |                       |                      |
      v                       v                       v                      v
+-------------+       +---------------+       +---------------+      +---------------+
| kromium-    |       |   kromium-    |       |   kromium-    |      |   kromium-    |
|   compose   |       |     swing     |       |    javafx     |      |   swt / awt   |
+-------------+       +---------------+       +---------------+      +---------------+
      \                       \                       /                      /
       \                       \                     /                      /
        +------------------------------------------------------------------+
        |                      kromium-provider-jcef                       |
        |  (Chromium Embedded Framework Native Off-Screen Rendering Pipe)   |
        +------------------------------------------------------------------+
```

---

## 30-Second Quickstart

### 1. Add Dependencies (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("org.daviante.kromium:kromium-api:0.1.0-SNAPSHOT")
    implementation("org.daviante.kromium:kromium-provider-jcef:0.1.0-SNAPSHOT")
    
    // Choose your UI toolkit:
    implementation("org.daviante.kromium:kromium-compose:0.1.0-SNAPSHOT")
    // or: kromium-swing, kromium-javafx, kromium-swt, kromium-awt
}
```

### 2. Launch a Browser

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider

suspend fun startBrowser() {
    withContext(Dispatchers.IO) {
        // 1. Initialize engine
        val config = KromiumConfig.builder().build()
        val engine = JcefProvider.createEngine(config)
        engine.initialize()

        // 2. Create isolated client & browser
        val client = engine.createClient()
        val browser = client.createBrowser("https://github.com", KromiumUiFramework.COMPOSE)

        // 3. Control page
        browser.navigation.loadUrl("https://news.ycombinator.com")
    }
}
```

---

## Documentation

Comprehensive guides and API reference manuals are located in [`docs/`](docs/):

### Getting Started
- [**Documentation Hub**](docs/README.md)
- [**5-Minute Quickstart Guide**](docs/getting-started/quickstart.md)
- [**Multi-Framework UI Integration**](docs/getting-started/ui-frameworks.md)

### API Guides
- [**Navigation & Reactive State**](docs/api/navigation.md)
- [**Web Automation Engine**](docs/api/automation.md)
- [**Interactive Dialogs & Context Menus**](docs/api/dialogs-and-menus.md)
- [**Enterprise Networking & Proxies**](docs/api/network-and-proxies.md)
- [**DevTools, Diagnostics & Logging**](docs/api/devtools-and-logging.md)
- [**Downloads, Security & Permissions**](docs/api/downloads-and-security.md)
- [**Configuration Guide**](docs/api/configuration.md)

### Deployment & Publishing
- [**Cloudflare R2 Maven Repository Guide**](docs/deployment/r2-maven-repository.md)

---

## Module Index

| Module | Description |
| :--- | :--- |
| [`kromium-api`](kromium-api) | Public, provider-agnostic interfaces, facades, configurations, and models. |
| [`kromium-core`](kromium-core) | Core utilities, runtime helpers, and shared architecture models. |
| [`kromium-provider-jcef`](kromium-provider-jcef) | JCEF SPI implementation: native process management and OSR pipeline. |
| [`kromium-compose`](kromium-compose) | Compose Multiplatform native integration (`KromiumView`). |
| [`kromium-swing`](kromium-swing) | Native Swing canvas (`KromiumSwingCanvas`). |
| [`kromium-javafx`](kromium-javafx) | Native JavaFX canvas (`KromiumJavaFxCanvas`). |
| [`kromium-swt`](kromium-swt) | Native Eclipse SWT canvas (`KromiumSwtCanvas`). |
| [`kromium-awt`](kromium-awt) | Native AWT canvas (`KromiumAwtCanvas`). |

---

## License & Copyright

Kromium is open-source software licensed under the [Apache License, Version 2.0](LICENSE).  
Copyright © 2025–2026 [Daviante Group](https://github.com/daviantegroup). All rights reserved.

All external contributions are governed by our [Contributor License Agreement (CLA)](CONTRIBUTING.md).
