[Documentation Hub](../README.md) / Core API / **Browser Navigation & Reactive State**

---

# Browser Navigation & Reactive State API

The `browser.navigation` facade ([`KromiumNavigation`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/navigation/KromiumNavigation.kt)) provides high-level control over page navigation, history traversal, network settlement awaiting, and real-time reactive state observation.

---

## 1. Loading Web Content

### Asynchronous Loading (Fire-and-Forget)

Initiates navigation to a URL immediately without blocking the calling thread:

```kotlin
browser.navigation.loadUrl("https://github.com")
```

### Suspending Navigation with Lifecycle Awaiting

Suspends the calling coroutine until navigation reaches a specific [`NavigationStage`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/navigation/NavigationStage.kt):

```kotlin
import org.daviante.kromium.api.navigation.NavigationStage

// Suspends until main frame finishes loading (default: NavigationStage.LOADED)
val isLoaded = browser.navigation.loadUrl(
    url = "https://github.com",
    waitUntil = NavigationStage.LOADED,
    timeoutMs = 15_000L
)

if (isLoaded) {
    println("Page loaded successfully!")
} else {
    println("Page load timed out or failed.")
}
```

### Awaiting Network Settlement (`NETWORK_IDLE`)

Useful for Single-Page Applications (SPAs) like React, Angular, or Vue that fetch data dynamically after the initial HTML load:

```kotlin
// Suspends until document is ready AND all network requests have settled to zero
val isSettled = browser.navigation.loadUrl(
    url = "https://app.example.com/dashboard",
    waitUntil = NavigationStage.NETWORK_IDLE,
    timeoutMs = 20_000L
)
```

You can also await network idle independently at any point:

```kotlin
// Wait until network traffic remains at 0 for at least 500ms
val networkSettled = browser.navigation.waitForNetworkIdle(
    idleTimeMs = 500L,
    maxTimeoutMs = 10_000L
)
```

### Loading Raw HTML Content

Loads an HTML string directly into the viewport without requiring an HTTP server:

```kotlin
val html = """
    <!DOCTYPE html>
    <html>
    <head><style>body { font-family: sans-serif; background: #121212; color: white; }</style></head>
    <body>
        <h1>Hello from Kromium!</h1>
        <p>Embedded HTML rendered directly in memory.</p>
    </body>
    </html>
""".trimIndent()

browser.navigation.loadHtml(
    html = html,
    url = "https://my-app.internal/" // Virtual origin for resolving relative paths and cookies
)
```

### Java Interop (`CompletableFuture`)

For Java callers or codebases not using Kotlin coroutines:

```java
CompletableFuture<Boolean> future = browser.getNavigation().loadUrlAsync(
    "https://example.com",
    NavigationStage.LOADED,
    10000L
);

future.thenAccept(success -> {
    if (success) {
        System.out.println("Navigation completed!");
    }
});
```

---

## 2. History Traversal & Controls

```kotlin
// Check back/forward capability
val canBack: Boolean = browser.navigation.canGoBack()
val canForward: Boolean = browser.navigation.canGoForward()

// Navigate backwards / forwards
if (canBack) {
    browser.navigation.goBack()
}
if (canForward) {
    browser.navigation.goForward()
}

// Reload current page (standard reload)
browser.navigation.reload()

// Hard reload (bypassing browser HTTP cache)
browser.navigation.reload(ignoreCache = true)

// Stop in-progress page loading
browser.navigation.stopLoad()
```

---

## 3. Reactive StateFlow Observation

The browser exposes `browser.navigation.navigationState`, a hot coroutine `StateFlow<KromiumNavigationState>` that continuously emits the latest snapshot of the browser's navigation lifecycle.

### State Model (`KromiumNavigationState`)

| Property | Type | Description |
| :--- | :--- | :--- |
| `isLoading` | `Boolean` | `true` if a page load or asset transfer is currently in progress. |
| `canGoBack` | `Boolean` | `true` if backwards history traversal is available. |
| `canGoForward` | `Boolean` | `true` if forwards history traversal is available. |
| `url` | `String` | Current committed URL of the main frame. |
| `title` | `String` | Current document title (`<title>`). |

### Collecting State in Compose Multiplatform

```kotlin
@Composable
fun BrowserTitleBar(browser: KromiumBrowser) {
    val navState by browser.navigation.navigationState.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = if (navState.isLoading) "Loading: ${navState.url}" else navState.title)
        if (navState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
    }
}
```

### Collecting State in Swing / JavaFX (Coroutine Scope)

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.swing.SwingUtilities

val uiScope = CoroutineScope(Dispatchers.Main)

uiScope.launch {
    browser.navigation.navigationState.collectLatest { state ->
        // Dispatch UI updates onto the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater {
            addressBar.text = state.url
            backButton.isEnabled = state.canGoBack
            forwardButton.isEnabled = state.canGoForward
            loadingProgressBar.isVisible = state.isLoading
            windowTitleLabel.text = state.title.ifEmpty { "New Tab" }
        }
    }
}
```

---

## 4. Handling Navigation & Load Errors

To capture network failures (e.g., DNS lookup failures, connection resets, SSL errors), attach a [`KromiumLoadErrorListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/navigation/KromiumLoadErrorListener.kt) to the client session:

```kotlin
import org.daviante.kromium.api.navigation.KromiumLoadErrorListener

// Attach to client directly:
client.loadErrorListener = KromiumLoadErrorListener { error ->
    println("Failed to load: ${error.failedUrl}")
    println("Error code: ${error.errorCode} — ${error.errorText}")
    
    // Optionally load an offline error page into the browser:
    browser.navigation.loadHtml(
        html = "<h1>Offline</h1><p>Could not connect to ${error.failedUrl} (${error.errorText})</p>",
        url = error.failedUrl
    )
}
```

Or configure it during client creation via builder:

```kotlin
val clientConfig = KromiumClientConfig.builder()
    .loadErrorListener { error ->
        System.err.println("Load error on ${error.failedUrl}: ${error.errorText}")
    }
    .build()

val client = engine.createClient(clientConfig)
```

---

## 5. Complete Example: Automated Page Crawler

```kotlin
package com.example.crawler

import kotlinx.coroutines.runBlocking
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.createHeadlessBrowser
import org.daviante.kromium.api.navigation.NavigationStage
import org.daviante.kromium.jcef.JcefProvider

fun main() = runBlocking {
    val config = KromiumConfig.builder()
        .browserConfig { it.isHeadless(true) }
        .build()

    val engine = JcefProvider.createEngine(config)
    engine.initialize()

    val browser = engine.createHeadlessBrowser(width = 1920, height = 1080)

    try {
        println("Navigating to target page...")
        val loaded = browser.navigation.loadUrl(
            url = "https://news.ycombinator.com",
            waitUntil = NavigationStage.NETWORK_IDLE,
            timeoutMs = 15_000L
        )

        if (loaded) {
            val title = browser.navigation.navigationState.value.title
            println("Crawled Page Title: $title")
            
            // Extract page text via automation engine:
            val headline = browser.automation.queryText(".titleline > a")
            println("Top Headline: $headline")
        }
    } finally {
        browser.close()
        engine.dispose()
    }
}
```

---

## Navigation

- [← Previous: Multi-Framework UI Integration](../getting-started/ui-frameworks.md)
- [Home: Documentation Hub](../README.md)
- [Next: Web Automation Engine →](automation.md)
