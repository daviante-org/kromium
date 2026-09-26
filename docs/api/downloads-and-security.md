[Documentation Hub](../README.md) / Core API / **Downloads, Security & Permissions**

---

# Downloads, Permissions & SSL Security API

Kromium provides full control over file download lifecycle, camera/microphone device permissions, and SSL/TLS certificate validation policies.

---

## 1. File Download Management

Download operations in Kromium are handled via two listeners on the `KromiumClient`:
1. [`KromiumBeforeDownloadListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/download/KromiumBeforeDownloadListener.kt): Decides the destination file path or cancels the download before disk write begins.
2. [`KromiumDownloadListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/download/KromiumDownloadListener.kt): Emits real-time progress, download speed, and completion updates.

### Download Lifecycle Interception

```kotlin
import org.daviante.kromium.api.download.KromiumBeforeDownloadListener
import org.daviante.kromium.api.download.KromiumDownloadListener
import java.io.File

val downloadsDir = File(System.getProperty("user.home"), "Downloads")

// 1. Intercept before download starts to specify custom file path or cancel
client.beforeDownloadListener = KromiumBeforeDownloadListener { item, suggestedFileName ->
    println("Incoming download: $suggestedFileName from ${item.url}")
    
    // Validate file extensions:
    if (suggestedFileName.endsWith(".exe") || suggestedFileName.endsWith(".bat")) {
        println("Blocked executable download for security.")
        return@KromiumBeforeDownloadListener "" // Return empty string to cancel
    }

    // Return target absolute path on disk (or null to use system default)
    File(downloadsDir, suggestedFileName).absolutePath
}

// 2. Track download progress, transfer speed, and completion
client.downloadListener = KromiumDownloadListener { item ->
    println("Download #${item.id} [${item.suggestedFileName}]: ${item.percentComplete}% (${item.receivedBytes}/${item.totalBytes} bytes) Speed: ${item.speed / 1024} KB/s")

    if (item.isComplete) {
        println("Download complete! Saved to: ${item.fullPath}")
    } else if (item.isCanceled) {
        println("Download canceled.")
    }
}
```

### Download Metadata (`KromiumDownloadItem`)

| Property | Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Unique integer identifier for this download operation. |
| `url` | `String` | Source URL of the downloaded resource. |
| `suggestedFileName` | `String` | Suggested filename sent in HTTP `Content-Disposition` headers or URL path. |
| `totalBytes` | `Long` | Total expected byte count (`-1` if `Content-Length` unknown). |
| `receivedBytes` | `Long` | Number of bytes received so far. |
| `percentComplete` | `Int` | Progress percentage (`0..100`), or `-1` if total size unknown. |
| `speed` | `Long` | Current download rate in bytes per second. |
| `isInProgress` | `Boolean` | `true` while bytes are actively transferring. |
| `isComplete` | `Boolean` | `true` when download finishes and file is sealed on disk. |
| `isCanceled` | `Boolean` | `true` if download was canceled by user or host application. |
| `isPaused` | `Boolean` | `true` if download is currently paused. |
| `fullPath` | `String` | Final absolute destination path on the local filesystem. |

### Programmatic Download Control (`browser.downloads`)

You can control active downloads programmatically through the `browser.downloads` facade ([`KromiumDownloads`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/download/KromiumDownloads.kt)):

```kotlin
// Manually initiate a file download
browser.downloads.startDownload("https://example.com/assets/report.pdf")

// Pause an in-progress download
browser.downloads.pauseDownload(downloadId = 42)

// Resume a paused download
browser.downloads.resumeDownload(downloadId = 42)

// Cancel an active download
browser.downloads.cancelDownload(downloadId = 42)
```

---

## 2. Media & Hardware Permissions

Modern web applications often request access to cameras, microphones, or screen sharing (e.g. Google Meet, Zoom web clients). Kromium delegates these permission requests to [`KromiumPermissionListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/permission/KromiumPermissionListener.kt).

### Permission Types (`KromiumPermissionType`)

- `AUDIO_CAPTURE`: Microphone / audio input device access.
- `VIDEO_CAPTURE`: Webcam / camera input device access.
- `DESKTOP_AUDIO`: Desktop audio capture.
- `DESKTOP_VIDEO`: Screen sharing / desktop window capture.

### Turnkey Permission Presets

```kotlin
import org.daviante.kromium.api.permission.KromiumPermissionListener

// 1. Grant all device permission requests unconditionally
client.permissionListener = KromiumPermissionListener.grantAll()

// 2. Deny all device permission requests (Kiosk / Locked Down mode)
client.permissionListener = KromiumPermissionListener.denyAll()

// 3. Grant permissions only to trusted corporate origins:
client.permissionListener = KromiumPermissionListener.forOrigins(
    "https://meet.google.com",
    "https://app.company.internal"
)
```

### Custom Granular Permission Evaluation

```kotlin
import org.daviante.kromium.api.permission.KromiumPermissionDecision
import org.daviante.kromium.api.permission.KromiumPermissionType

client.permissionListener = KromiumPermissionListener { request ->
    println("Origin '${request.origin}' requested permissions: ${request.types}")

    if (request.origin.startsWith("https://app.corp.internal")) {
        // Grant only microphone, deny camera:
        KromiumPermissionDecision.grant(KromiumPermissionType.AUDIO_CAPTURE)
    } else {
        // Deny untrusted origins
        KromiumPermissionDecision.DENY
    }
}
```

---

## 3. SSL / TLS Certificate Validation Policies

By default, Kromium enforces strict enterprise-grade SSL/TLS certificate validation. You can customize the certificate error policy via [`SslErrorPolicy`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/network/SslErrorPolicy.kt).

### Strict Mode (Default — Recommended for Production)

Rejects all expired, self-signed, or invalid SSL certificates:

```kotlin
import org.daviante.kromium.api.network.SslErrorPolicy

client.sslErrorPolicy = SslErrorPolicy.Strict
```

### Domain Whitelist (Self-Signed Internal Servers)

In enterprise intranets or local development setups, services often use self-signed certificates or internal root Certificate Authorities. You can selectively allow SSL errors only for specific domains:

```kotlin
client.sslErrorPolicy = SslErrorPolicy.AllowDomains(
    "localhost",
    "127.0.0.1",
    "*.dev.corp.internal",
    "staging.company.net"
)
```

### Allow All (Development Only)

> **⚠️ WARNING:** `AllowAll` completely disables TLS certificate validation. Never use in production.

```kotlin
client.sslErrorPolicy = SslErrorPolicy.AllowAll
```

---

## Navigation

- [← Previous: DevTools, Diagnostics & Logging](devtools-and-logging.md)
- [Home: Documentation Hub](../README.md)
- [Next: Configuration Guide →](configuration.md)
