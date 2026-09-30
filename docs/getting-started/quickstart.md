[Documentation Hub](../README.md) / Getting Started / **5-Minute Quickstart Guide**

---

# 5-Minute Quickstart Guide

This guide will walk you through adding Kromium to your Gradle project, bootstrapping the Chromium engine, and creating your first browser instance.

---

## Prerequisites

- **JDK:** Java 17 or higher (Java 21 LTS recommended, e.g., Eclipse Temurin).
- **Supported Operating Systems:**
  - **Windows:** 10 / 11 (x86_64, ARM64)
  - **macOS:** 12+ Monterey, Ventura, Sonoma, Sequoia (Intel x86_64, Apple Silicon ARM64)
  - **Linux:** Ubuntu 20.04+, Debian 11+, Fedora, Arch (x86_64, ARM64) with GTK 3+ installed.

---

## 1. Adding Gradle Dependencies

Add Kromium to your `build.gradle.kts`:

### Option A: Using Gradle Version Catalog (`libs.versions.toml`)

```toml
[versions]
kromium = "0.1.0-SNAPSHOT"

[libraries]
kromium-api = { module = "org.daviante.kromium:kromium-api", version.ref = "kromium" }
kromium-provider-jcef = { module = "org.daviante.kromium:kromium-provider-jcef", version.ref = "kromium" }
# Choose your UI canvas module:
kromium-compose = { module = "org.daviante.kromium:kromium-compose", version.ref = "kromium" }
# or kromium-swing, kromium-javafx, kromium-swt, kromium-awt
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.kromium.api)
    implementation(libs.kromium.provider.jcef)
    
    // Choose your UI framework integration:
    implementation(libs.kromium.compose)
    // implementation(libs.kromium.swing)
    // implementation(libs.kromium.javafx)
    // implementation(libs.kromium.swt)
    // implementation(libs.kromium.awt)
}
```

### Option B: Direct Coordinates

```kotlin
dependencies {
    val kromiumVersion = "0.1.0-SNAPSHOT"
    
    implementation("org.daviante.kromium:kromium-api:$kromiumVersion")
    implementation("org.daviante.kromium:kromium-provider-jcef:$kromiumVersion")

    // Target UI integration:
    implementation("org.daviante.kromium:kromium-compose:$kromiumVersion")
    // implementation("org.daviante.kromium:kromium-swing:$kromiumVersion")
    // implementation("org.daviante.kromium:kromium-javafx:$kromiumVersion")
    // implementation("org.daviante.kromium:kromium-swt:$kromiumVersion")
    // implementation("org.daviante.kromium:kromium-awt:$kromiumVersion")
}
```

---

## 2. Basic Engine Lifecycle & First Browser

Kromium separates the engine lifecycle into three clean stages:
1. **`KromiumEngine`**: Manages the underlying native Chromium runtime, process models, and shared cache.
2. **`KromiumClient`**: Represents an isolated session (cookies, storage, and global listeners).
3. **`KromiumBrowser`**: Represents a single web page view and its control facades.

### Minimal Kotlin Example

```kotlin
package com.example.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.JcefProvider
import java.io.File

class BrowserManager {
    private var engine: KromiumEngine? = null
    private var client: KromiumClient? = null
    var browser: KromiumBrowser? = null
        private set

    /**
     * Initializes the engine and creates the first browser instance.
     * Note: Initialization should be dispatched off the UI thread.
     */
    suspend fun start(targetFramework: KromiumUiFramework = KromiumUiFramework.SWING) {
        withContext(Dispatchers.IO) {
            // 1. Build configuration
            val config = KromiumConfig.builder()
                .browserConfig { b ->
                    b.cachePath(File(System.getProperty("user.home"), ".myapp-cache").absolutePath)
                }
                .build()

            // 2. Instantiate and initialize Chromium engine
            val initializedEngine = JcefProvider.createEngine(config)
            initializedEngine.initialize() // Suspends until native runtime is extracted and ready

            // 3. Create isolated client session
            val createdClient = initializedEngine.createClient()

            // 4. Create browser instance for your UI toolkit
            val createdBrowser = createdClient.createBrowser(
                url = "https://example.com",
                framework = targetFramework
            )

            engine = initializedEngine
            client = createdClient
            browser = createdBrowser
        }
    }

    /**
     * Cleanly disposes browser and engine native resources on application shutdown.
     */
    fun shutdown() {
        browser?.close()
        engine?.dispose()
        browser = null
        client = null
        engine = null
    }
}
```

---

## 3. Controlling the Browser

Once created, `browser` exposes dedicated facades for all web operations:

```kotlin
// --- Navigation ---
// Non-suspending load:
browser.navigation.loadUrl("https://github.com")

// Suspending load (waits until DOM is ready):
val loaded = browser.navigation.loadUrl(
    url = "https://github.com",
    waitUntil = NavigationStage.LOADED,
    timeoutMs = 15_000L
)

// History controls:
if (browser.navigation.canGoBack()) {
    browser.navigation.goBack()
}
browser.navigation.reload(ignoreCache = true)

// --- DevTools ---
// Open full Chrome DevTools window:
browser.devTools.openDevTools()

// --- Console Log Interception ---
browser.devTools.addConsoleMessageListener { msg ->
    println("[${msg.level}] ${msg.source}:${msg.line} -> ${msg.message}")
    false // Return true to suppress printing to standard console
}
```

---

## 4. Observing Reactive Navigation State

`browser.navigation.navigationState` is a coroutine `StateFlow<KromiumNavigationState>` that continuously emits real-time loading updates:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val scope = CoroutineScope(Dispatchers.Main)

scope.launch {
    browser.navigation.navigationState.collect { state ->
        println("URL: ${state.url}")
        println("Page Title: ${state.title}")
        println("Is Loading: ${state.isLoading}")
        println("Can Go Back: ${state.canGoBack}")
        println("Can Go Forward: ${state.canGoForward}")
        println("HTTP Status: ${state.httpStatusCode}")
    }
}
```

---

## Navigation

- [← Home: Documentation Hub](../README.md)
- [Next: Multi-Framework UI Integration →](ui-frameworks.md)
