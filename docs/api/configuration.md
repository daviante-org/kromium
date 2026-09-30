[Documentation Hub](../README.md) / Core API / **Configuration Guide**

---

# Configuration Guide

Kromium uses strongly typed, immutable configuration objects composed through fluent builders. Configuration is separated into two tiers:
1. **[`KromiumConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumConfig.kt)**: Engine-wide settings applied once when the native Chromium runtime is initialized (cache directory, sandboxing, GPU acceleration, CLI switches).
2. **[`KromiumClientConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumClientConfig.kt)**: Session-specific settings and event listeners for each isolated browser client.

---

## 1. Engine Master Configuration (`KromiumConfig`)

Constructed via `KromiumConfig.builder()`:

```kotlin
import org.daviante.kromium.api.config.*
import org.daviante.kromium.api.proxy.KromiumProxy
import java.io.File

val config = KromiumConfig.builder()
    // 1. Browser & Process Behavior
    .browserConfig { b ->
        b.cachePath(File(System.getProperty("user.home"), ".myapp/cache").absolutePath)
        b.isHeadless(false)
        b.remoteDebuggingPort(0) // 0 = disabled, or e.g. 9222 for DevTools CDP
        b.proxy(KromiumProxy.Direct)
        b.addArgs(
            "--disable-background-networking",
            "--disable-sync"
        )
    }

    // 2. Hardware & GPU Acceleration
    .hardwareConfig { h ->
        h.gpuMode(KromiumGpuMode.HARDWARE) // HARDWARE, SOFTWARE, or DISABLED
        h.processModel(KromiumProcessModel.PROCESS_PER_SITE)
    }

    // 3. Security & Telemetry
    .securityConfig { s ->
        s.sandboxEnabled(true) // Native Chromium OS sandboxing
        s.blockRegistryAndTelemetry(true)
        s.doNotTrack(true)
    }

    // 4. Logging & Diagnostics
    .loggingConfig { l ->
        l.logSeverity(KromiumLogSeverity.WARNING) // DEFAULT, VERBOSE, INFO, WARNING, ERROR, DISABLE
    }

    // 5. Engine Runtime Download & Extraction
    .downloadConfig { d ->
        d.autoDownload(true)
        d.installDir(File(System.getProperty("user.home"), ".myapp/chromium-bin").absolutePath)
    }
    .build()
```

### Configuration Options Breakdown

#### `browserConfig` ([`KromiumBrowserConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumBrowserConfig.kt))
- `cachePath(path: String)`: Local directory where Chromium stores cookies, cache, LocalStorage, and IndexedDB.
- `isHeadless(enabled: Boolean)`: When true, optimizes the engine for invisible automation/crawling.
- `remoteDebuggingPort(port: Int)`: Port for the Chrome DevTools Protocol (CDP).
- `proxy(proxy: KromiumProxy)`: Default proxy applied on startup.
- `addArgs(vararg args: String)`: Custom command-line flags forwarded to the Chromium subprocess.

#### `hardwareConfig` ([`KromiumHardwareConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumHardwareConfig.kt))
- `gpuMode(mode: KromiumGpuMode)`:
  - `KromiumGpuMode.HARDWARE`: Uses GPU hardware acceleration (recommended for standard desktop apps).
  - `KromiumGpuMode.SOFTWARE`: CPU software rasterization (useful for headless virtual machines / CI runners).
  - `KromiumGpuMode.DISABLED`: Disables GPU process completely.
- `processModel(model: KromiumProcessModel)`: Controls multi-process separation (`PROCESS_PER_SITE`, `PROCESS_PER_TAB`, `SINGLE_PROCESS`).

#### `securityConfig` ([`KromiumSecurityConfig`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/config/KromiumSecurityConfig.kt))
- `sandboxEnabled(enabled: Boolean)`: Controls Chromium's multi-process OS sandbox boundaries.
- `blockRegistryAndTelemetry(enabled: Boolean)`: Strips Google crash reporter and metrics reporting.
- `doNotTrack(enabled: Boolean)`: Automatically sends `DNT: 1` headers with all HTTP requests.

---

## 2. Client Session Configuration (`KromiumClientConfig`)

Passed when creating a `KromiumClient` (`engine.createClient(clientConfig)`):

```kotlin
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.network.SslErrorPolicy
import org.daviante.kromium.api.ui.KromiumUiFramework

val clientConfig = KromiumClientConfig.builder()
    .framework(KromiumUiFramework.COMPOSE) // Target UI toolkit
    .isTransparent(true)                   // Enables alpha transparency for translucent UI
    .initialWidth(1280)
    .initialHeight(800)
    .sslErrorPolicy(SslErrorPolicy.Strict)
    
    // Attach default session listeners:
    .loadErrorListener { error -> 
        System.err.println("Page load failed: ${error.failedUrl}")
    }
    .downloadListener { item -> 
        println("Download: ${item.percentComplete}%")
    }
    .permissionListener(KromiumPermissionListener.grantAll())
    .contextMenuListener(KromiumContextMenuListener.minimalEditing(includeInspectElement = true))
    .build()

val client = engine.createClient(clientConfig)
```

---

## 3. Production Best Practices

1. **Explicit Cache Directories:** Always set a dedicated, application-specific `cachePath` in your user data directory so sessions do not conflict with other Chromium applications.
2. **Never Disable Sandboxing in Production:** Disabling `sandboxEnabled` exposes the host operating system to vulnerabilities if loading untrusted web content.
3. **Dispatch Initialization to Background:** Calling `engine.initialize()` extracts native binaries and spawns Chromium helper processes. Always invoke it from a background coroutine (`Dispatchers.IO`) to avoid freezing the UI.
4. **Clean Shutdown:** Register a shutdown hook or window closing listener to call `browser.close()` followed by `engine.dispose()`.

---

## Summary of Guides

You have explored the full developer API surface of Kromium:
- [**5-Minute Quickstart**](../getting-started/quickstart.md)
- [**Multi-Framework UI Integration**](../getting-started/ui-frameworks.md)
- [**Browser Navigation & Reactive State**](navigation.md)
- [**Web Automation Engine**](automation.md)
- [**Interactive Dialogs & Context Menus**](dialogs-and-menus.md)
- [**Enterprise Networking & Proxies**](network-and-proxies.md)
- [**DevTools, Diagnostics & Logging**](devtools-and-logging.md)
- [**Downloads, Security & Permissions**](downloads-and-security.md)

---

## Navigation

- [← Previous: Downloads, Security & Permissions](downloads-and-security.md)
- [Home: Documentation Hub](../README.md)
- [Next: Cloudflare R2 Maven Repository →](../deployment/r2-maven-repository.md)
