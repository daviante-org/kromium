# Kromium Documentation Hub

Welcome to the **Kromium** documentation portal. Kromium is a modern, high-performance, developer-first Chromium browser engine for Kotlin and Java desktop applications, providing native off-screen rendering (OSR) across all major JVM desktop toolkits.

---

## Key Highlights

- **Decoupled Modern Architecture:** Public developer surface ([`kromium-api`](../kromium-api)) is 100% provider-agnostic with zero native JCEF leaks, exposing cohesive, domain-driven facades.
- **Universal Desktop UI Embedding:** First-class native canvas support for:
  - **Compose Multiplatform** (`kromium-compose` with `KromiumView` and custom loading slots)
  - **Swing** (`kromium-swing` with `KromiumSwingCanvas`)
  - **JavaFX** (`kromium-javafx` with `KromiumJavaFxCanvas`)
  - **SWT** (`kromium-swt` with `KromiumSwtCanvas`)
  - **AWT** (`kromium-awt` with `KromiumAwtCanvas`)
- **Reactive Coroutine Streams:** Full observability with `StateFlow<KromiumNavigationState>` tracking real-time loading stages (`DOM_CONTENT_LOADED`, `LOADED`, `NETWORK_IDLE`), progress, and page metadata.
- **Enterprise Web Automation:** Built-in automation facade (`browser.automation`) supporting React/Vue/Angular controlled components, progressive human-like typing with micro-delays, and in-V8 `MutationObserver`-driven element resolution without IPC polling lag.
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

## Table of Contents

### 1. Getting Started
- [**5-Minute Quickstart Guide**](getting-started/quickstart.md): Add dependencies, initialize the engine, and launch your first browser.
- [**Multi-Framework UI Integration**](getting-started/ui-frameworks.md): Comprehensive embedding guides for Compose Multiplatform, Swing, JavaFX, SWT, and AWT.

### 2. Core API Reference
- [**Navigation & Reactive State**](api/navigation.md): URL loading, navigation stages, history traversal, and coroutine state collection.
- [**Web Automation Engine**](api/automation.md): Controlled inputs, progressive typing, clicks, and MutationObserver element waiting.
- [**Interactive Dialogs & Context Menus**](api/dialogs-and-menus.md): JavaScript alert/confirm/prompt handling and custom right-click menus.
- [**Enterprise Networking & Proxies**](api/network-and-proxies.md): HTTP/SOCKS5 proxies, bypass lists, and HTTP authentication.
- [**DevTools, Diagnostics & Logging**](api/devtools-and-logging.md): Opening Chrome DevTools, inspect coordinates, and capturing console messages.
- [**Downloads, Security & Permissions**](api/downloads-and-security.md): Handling file downloads, device permissions, and SSL certificate errors.
- [**Configuration Guide**](api/configuration.md): Engine master configuration (`KromiumConfig`), client session configuration (`KromiumClientConfig`), and CLI switches.

### 3. Deployment & Operations
- [**Cloudflare R2 Maven Repository**](deployment/r2-maven-repository.md): Build automation, CI/CD artifact publishing, and consuming from R2.

---

## Module Index

| Module | Description |
| :--- | :--- |
| [`kromium-api`](../kromium-api) | Public, provider-agnostic interfaces, facades, configurations, and models. Zero CEF dependencies. |
| [`kromium-core`](../kromium-core) | Core utilities, runtime helpers, and shared architecture models. |
| [`kromium-provider-jcef`](../kromium-provider-jcef) | JCEF SPI implementation: native process management, OSR Java2D rendering pipeline, and CEF adapters. |
| [`kromium-compose`](../kromium-compose) | Compose Multiplatform native integration (`KromiumView`) with reactive state binding. |
| [`kromium-swing`](../kromium-swing) | Native Swing canvas (`KromiumSwingCanvas`) with double-buffered Java2D rendering. |
| [`kromium-javafx`](../kromium-javafx) | Native JavaFX canvas (`KromiumJavaFxCanvas`) for JavaFX Scene integration. |
| [`kromium-swt`](../kromium-swt) | Native Eclipse SWT canvas (`KromiumSwtCanvas`) for SWT desktop shells. |
| [`kromium-awt`](../kromium-awt) | Native AWT canvas (`KromiumAwtCanvas`) for lightweight AWT containers. |

---

## Navigation

- [Next: 5-Minute Quickstart Guide →](getting-started/quickstart.md)
