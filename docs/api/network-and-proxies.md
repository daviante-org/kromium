[Documentation Hub](../README.md) / Core API / **Enterprise Networking & Proxies**

---

# Enterprise Networking, Proxies & Authentication API

Kromium provides enterprise-grade networking facilities, from granular proxy routing and dynamic runtime switching to automatic HTTP authentication and host locking.

---

## 1. Proxy Architecture & Strategies

Kromium models proxy configurations via the type-safe sealed class [`KromiumProxy`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/proxy/KromiumProxy.kt).

### Supported Proxy Modes

| Mode | Class | Description |
| :--- | :--- | :--- |
| **System Default** | `KromiumProxy.System` | Adopts the host operating system's configured proxy settings. |
| **Direct** | `KromiumProxy.Direct` | Bypasses all proxies and connects directly to destination hosts. |
| **Auto-Detect** | `KromiumProxy.AutoDetect` | Uses Web Proxy Auto-Discovery (WPAD) via DHCP and DNS queries. |
| **PAC Script** | `KromiumProxy.Pac(pacUrl)` | Downloads and evaluates a Proxy Auto-Configuration script. |
| **HTTP / HTTPS** | `KromiumProxy.Http(...)` | Standard HTTP or TLS-encrypted HTTPS forward proxy tunnel. |
| **SOCKS5 / SOCKS4** | `KromiumProxy.Socks5(...)` | SOCKS proxy with optional remote DNS leak protection. |
| **Multi-Protocol** | `KromiumProxy.MultiProtocol(...)` | Routes different protocols (`http`, `https`, `socks`) through separate endpoints. |

---

## 2. Configuring Proxies

### Startup Proxy (Applied at Engine Launch)

Configure the default proxy when building the `KromiumConfig`:

```kotlin
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.proxy.KromiumProxy

val config = KromiumConfig.builder()
    .browserConfig { b ->
        b.proxy(
            KromiumProxy.Http(
                host = "proxy.corp.internal",
                port = 8080,
                username = "corporate_user",
                password = "secure_password",
                isSecure = true, // TLS encrypted proxy tunnel (https://)
                bypassList = listOf("<local>", "127.0.0.1", "*.internal.net")
            )
        )
    }
    .build()
```

### SOCKS5 Proxy with Remote DNS (Leak Prevention)

When routing traffic through SOCKS5, local DNS requests can leak visited hostnames to your local ISP. Setting `remoteDns = true` ensures DNS queries are resolved exclusively through the proxy:

```kotlin
val socks5Proxy = KromiumProxy.Socks5(
    host = "10.200.0.1",
    port = 1080,
    username = "socks_user",
    password = "socks_password",
    remoteDns = true, // Prevents DNS leaks (socks5:// scheme)
    bypassList = listOf("localhost", "127.0.0.1")
)
```

### Multi-Protocol Routing

In complex enterprise networks, HTTP traffic and HTTPS traffic often egress through distinct proxy clusters:

```kotlin
val multiProxy = KromiumProxy.MultiProtocol(
    http = "http://http-proxy.corp.internal:8080",
    https = "https://secure-proxy.corp.internal:8443",
    socks = "socks5://socks-gateway.corp.internal:1080",
    bypassList = listOf("<local>", "192.168.0.0/16")
)
```

### Dynamic Runtime Proxy Switching

Unlike standard browser engines that require restarting the entire application to change proxies, Kromium supports updating the proxy dynamically at runtime via [`KromiumProxyManager`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/proxy/KromiumProxyManager.kt):

```kotlin
// Switch active browser to a new proxy immediately:
val updated: Boolean = browser.proxyManager.updateProxy(
    KromiumProxy.Http(host = "egress-eu.proxy.net", port = 3128)
)

if (updated) {
    println("Switched proxy successfully without restarting the engine!")
}

// Revert to direct connection:
browser.proxyManager.updateProxy(KromiumProxy.Direct)
```

---

## 3. Proxy Bypass Syntax & Rules

The `bypassList` accepts standard Chromium bypass rules separated by semicolons:

- `<local>`: Matches any single-label host without dots (e.g. `http://intranet/`, `http://wiki/`).
- `127.0.0.1`, `localhost`: Loopback bypass.
- `*.corp.domain.com`: Matches all subdomains.
- `192.168.1.0/24`: CIDR subnet block matching.
- `<-loopback>`: Negation rule (forces loopback addresses through the proxy).

---

## 4. HTTP & Proxy Authentication

When a web server or proxy issues an HTTP 401 (Unauthorized) or HTTP 407 (Proxy Authentication Required) challenge, Kromium delegates the credential resolution to [`KromiumAuthListener`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/network/KromiumAuthListener.kt).

### Registering the Auth Listener

```kotlin
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.KromiumAuthRequest
import org.daviante.kromium.api.network.KromiumAuthResponse

client.authListener = KromiumAuthListener { request: KromiumAuthRequest ->
    println("Auth Challenge Received:")
    println("  Is Proxy: ${request.isProxy}")
    println("  Host: ${request.host}:${request.port}")
    println("  Realm: ${request.realm}")
    println("  Scheme: ${request.scheme}")

    if (request.isProxy) {
        // Supply credentials for authenticated enterprise proxy
        KromiumAuthResponse.Proceed(
            username = "proxy_service_account",
            password = "EnterprisePassword"
        )
    } else if (request.host == "internal-tools.corp.com") {
        // Supply credentials for internal intranet site
        KromiumAuthResponse.Proceed(
            username = "admin",
            password = "SitePassword"
        )
    } else {
        // Cancel challenge for untrusted hosts
        KromiumAuthResponse.Cancel
    }
}
```

---

## 5. Enterprise Security: Host Locking

The `browser.security` facade ([`KromiumSecurity`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/network/KromiumSecurity.kt)) enables strict kiosk and locked-down enterprise browser modes by enforcing domain whitelists:

```kotlin
// Whitelist allowed top-level navigation domains
browser.security.hostLock = setOf(
    "app.mycompany.com",
    "auth.mycompany.com",
    "cdn.mycompany.com"
)

// Block all subresource requests (scripts, images, fetch, xhr) outside the whitelist
browser.security.hostLockSubresources = true

// Allow embedded third-party verification widgets (e.g. CAPTCHA, payment gateways)
// to navigate within iframes without violating top-level host lock:
browser.security.hostLockSubframes = false
```

---

## 6. Complete Example: Secure Enterprise Kiosk Session

```kotlin
package com.example.enterprise

import kotlinx.coroutines.runBlocking
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.createHeadlessBrowser
import org.daviante.kromium.api.network.KromiumAuthListener
import org.daviante.kromium.api.network.KromiumAuthResponse
import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.jcef.JcefProvider

fun main() = runBlocking {
    // 1. Configure SOCKS5 proxy with remote DNS resolution
    val config = KromiumConfig.builder()
        .browserConfig { b ->
            b.proxy(
                KromiumProxy.Socks5(
                    host = "gateway.corp.internal",
                    port = 1080,
                    remoteDns = true,
                    bypassList = listOf("<local>", "127.0.0.1")
                )
            )
        }
        .build()

    val engine = JcefProvider.createEngine(config)
    engine.initialize()

    val client = engine.createClient()

    // 2. Attach automatic authentication handler
    client.authListener = KromiumAuthListener { req ->
        if (req.isProxy) {
            KromiumAuthResponse.Proceed("service_user", "Pass1234!")
        } else {
            KromiumAuthResponse.Cancel
        }
    }

    val browser = client.createBrowser("https://app.corp.internal")

    // 3. Enforce strict domain whitelisting
    browser.security.hostLock = setOf("app.corp.internal", "sso.corp.internal")
    browser.security.hostLockSubresources = true

    println("Enterprise session initialized with SOCKS5, auto-auth, and host locking.")
}
```

---

## Navigation

- [← Previous: Interactive Dialogs & Context Menus](dialogs-and-menus.md)
- [Home: Documentation Hub](../README.md)
- [Next: DevTools, Diagnostics & Logging →](devtools-and-logging.md)
