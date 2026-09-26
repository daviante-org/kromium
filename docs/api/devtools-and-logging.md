[Documentation Hub](../README.md) / Core API / **DevTools, Diagnostics & Logging**

---

# DevTools, Diagnostics & Logging API

Kromium provides integrated diagnostic facilities, including an embedded Chrome DevTools window, coordinate-based element inspection, structured JavaScript console message interception, and remote debugging via the Chrome DevTools Protocol (CDP).

---

## 1. Controlling Chrome DevTools

The `browser.devTools` facade ([`KromiumDevTools`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/devtools/KromiumDevTools.kt)) allows opening and closing the native Chromium developer tools window.

### Opening the Inspector

```kotlin
// Open standard Chrome DevTools window in a standalone desktop window
browser.devTools.openDevTools()
```

### Element Inspection at Specific Coordinates

To open the Elements panel targeting a specific DOM element (e.g. right-click "Inspect Element"):

```kotlin
import java.awt.Point

// Open DevTools with the Elements panel focused on coordinates (x=450, y=320)
browser.devTools.openDevTools(inspectPoint = Point(450, 320))
```

### Closing the Inspector

```kotlin
browser.devTools.closeDevTools()
```

---

## 2. Intercepting JavaScript Console Messages

Webpages frequently log errors, warnings, and diagnostic telemetry via `console.log()`, `console.warn()`, and `console.error()`. Kromium captures these messages and surfaces them through [`KromiumConsoleMessageListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/devtools/KromiumConsoleMessageListener.kt).

### The Console Message Model (`KromiumConsoleMessage`)

| Property | Type | Description |
| :--- | :--- | :--- |
| `level` | `KromiumConsoleMessageLevel` | Severity level: `DEBUG`, `INFO`, `WARNING`, `ERROR`, or `DEFAULT`. |
| `message` | `String` | Raw string content logged to the console. |
| `source` | `String` | Source script URL or filename that emitted the message. |
| `line` | `Int` | Line number in the source file (1-based), or `0` if unknown. |

### Severity Levels (`KromiumConsoleMessageLevel`)

```kotlin
package org.daviante.kromium.api.devtools

enum class KromiumConsoleMessageLevel {
    DEBUG,   // console.debug(), verbose messages
    INFO,    // console.info(), console.log()
    WARNING, // console.warn()
    ERROR,   // console.error(), uncaught exceptions, network 4xx/5xx errors
    DEFAULT  // unspecified log levels
}
```

### Attaching the Console Listener

Attach the listener to your `KromiumClient`:

```kotlin
import org.daviante.kromium.api.devtools.KromiumConsoleMessageListener
import org.daviante.kromium.api.devtools.KromiumConsoleMessageLevel

client.consoleMessageListener = KromiumConsoleMessageListener { msg ->
    val location = if (msg.source.isNotBlank()) " [${msg.source}:${msg.line}]" else ""
    
    when (msg.level) {
        KromiumConsoleMessageLevel.ERROR -> {
            System.err.println("🔴 [JS ERROR]$location ${msg.message}")
        }
        KromiumConsoleMessageLevel.WARNING -> {
            println("🟡 [JS WARN]$location ${msg.message}")
        }
        KromiumConsoleMessageLevel.INFO -> {
            println("🔵 [JS INFO]$location ${msg.message}")
        }
        KromiumConsoleMessageLevel.DEBUG -> {
            println("⚪ [JS DEBUG]$location ${msg.message}")
        }
        KromiumConsoleMessageLevel.DEFAULT -> {
            println("⚫ [JS LOG]$location ${msg.message}")
        }
    }

    // Return true to suppress printing to standard stdout/stderr,
    // or false to allow the engine's default logging output.
    false
}
```

### Filtering Out Third-Party Console Noise

In production applications, third-party analytics or ads often flood the console with warnings. You can filter them out cleanly:

```kotlin
client.consoleMessageListener = KromiumConsoleMessageListener { msg ->
    // Ignore benign CORS or analytics warnings
    if (msg.message.contains("analytics.google.com") || msg.message.contains("Third-party cookie")) {
        return@KromiumConsoleMessageListener true // Suppress noise
    }

    // Capture critical application exceptions
    if (msg.level == KromiumConsoleMessageLevel.ERROR) {
        reportErrorToMonitoringService(msg.message, msg.source, msg.line)
    }

    false
}
```

---

## 3. Remote Debugging (Chrome DevTools Protocol - CDP)

Kromium can expose a remote debugging port, allowing external tools (such as Chrome, Puppeteer, or Playwright) to attach to the running desktop browser over WebSockets and HTTP.

### Enabling Remote Debugging Port

Configure `remoteDebuggingPort` when constructing `KromiumConfig`:

```kotlin
val config = KromiumConfig.builder()
    .browserConfig { b ->
        // Enable remote debugging on port 9222
        b.remoteDebuggingPort(9222)
    }
    .build()
```

### Connecting to the Browser

1. Start your Kromium application.
2. Open Google Chrome on your development machine and navigate to:
   ```
   chrome://inspect
   ```
3. Configure `localhost:9222` as a target. Your embedded Kromium web pages will appear in the target list with a full interactive DevTools session!
4. Alternatively, query available tabs via HTTP JSON:
   ```bash
   curl http://localhost:9222/json
   ```

---

## 4. Engine Native Log Severity

In addition to web console messages, the underlying Chromium engine emits native C++ process logs. You can configure the engine log verbosity via [`KromiumLoggingConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumLoggingConfig.kt):

```kotlin
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.config.KromiumLogSeverity

val config = KromiumConfig.builder()
    .loggingConfig { l ->
        // Options: DEFAULT, VERBOSE, INFO, WARNING, ERROR, FATAL, DISABLE
        l.logSeverity(KromiumLogSeverity.WARNING)
    }
    .build()
```

---

## Navigation

- [← Previous: Enterprise Networking & Proxies](network-and-proxies.md)
- [Home: Documentation Hub](../README.md)
- [Next: Downloads, Security & Permissions →](downloads-and-security.md)
