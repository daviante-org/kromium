[Documentation Hub](../README.md) / Core API / **Web Automation Engine**

---

# Web Automation & DOM Interaction API

Kromium features a modern, Playwright-inspired automation engine accessible via the `browser.automation` facade ([`KromiumAutomation`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/automation/KromiumAutomation.kt)) and `browser.jsBridge` ([`KromiumJsBridge`](../../kromium-api/src/main/kotlin/org/daviante/kromium/api/automation/KromiumJsBridge.kt)).

The engine is engineered specifically for modern Single-Page Applications (React, Vue, Angular, Svelte) to handle controlled components, debounce input listeners, and eliminate IPC polling overhead.

---

## 1. Waiting for Elements & URLs

### MutationObserver-Backed Selector Waiting

Unlike naive automation tools that repeatedly poll the DOM over IPC loops, `waitForSelector` injects a native in-V8 `MutationObserver`. It suspends your coroutine and resumes instantly the millisecond the target element appears in the DOM:

```kotlin
// Suspends until element matching CSS selector appears (or times out)
val elementFound = browser.automation.waitForSelector(
    selector = "#login-form",
    timeoutMs = 10_000L
)

if (elementFound) {
    println("Form is rendered and ready for interaction!")
}
```

### Awaiting URL Transitions

Waits for the page URL to match a specific string or regular expression:

```kotlin
// Wait for redirect to dashboard
val redirected = browser.automation.waitForUrl(
    pattern = "https://app.example.com/dashboard",
    timeoutMs = 15_000L
)

// Wait for URL matching a regex pattern
val matched = browser.automation.waitForUrl(
    pattern = "^https://app\\.example\\.com/orders/\\d+$",
    isRegex = true,
    timeoutMs = 15_000L
)
```

### Querying Element State

```kotlin
// Check visibility
val isVisible = browser.automation.isVisible("button.submit-btn")

// Check checkbox / radio button state
val isAgreed = browser.automation.isChecked("input#terms-checkbox")

// Count elements matching a selector
val productCount = browser.automation.count(".product-card")
println("Found $productCount products on page")
```

---

## 2. Interacting with Form Inputs

### Controlled Components (`fill`)

In modern reactive web frameworks (React 16+, Vue 3), inputs override the prototype property setter for `value`. Directly setting `input.value = "..."` is ignored by the framework's internal virtual DOM state tracker.

`browser.automation.fill` automatically resolves the native prototype setter descriptor (`HTMLInputElement.prototype` / `HTMLTextAreaElement.prototype`), updates the component value, and dispatches bubbling `'input'` and `'change'` DOM events:

```kotlin
// Instant value injection compatible with React/Vue controlled state
browser.automation.fill("#username", "john_doe")
browser.automation.fill("#password", "SecretP@ss123")
```

### Progressive Human-Like Typing (`type`)

For inputs with live search, auto-completion, or debounced keyup/keydown listeners, `browser.automation.type` progressively injects characters one-by-one with configurable micro-delays:

```kotlin
// Types with a 50ms delay between keystrokes to trigger live search
browser.automation.type(
    selector = "input#search-query",
    text = "Mechanical Keyboards",
    delayMs = 50L
)
```

### Dropdown Selection (`selectOption`)

Selects an option inside standard HTML `<select>` elements by its value:

```kotlin
browser.automation.selectOption("select#country-picker", "US")
```

---

## 3. Clicks & Mouse Simulation

### Selector-Based Clicking

Locates the target element, ensures it is rendered, and triggers a click:

```kotlin
val clicked = browser.automation.click("button[type='submit']")
```

### Coordinate-Based Clicking

Dispatches a simulated mouse click at precise viewport coordinates (useful for canvas applications or games):

```kotlin
browser.automation.simulateClick(x = 450, y = 320)
```

---

## 4. Extracting Data & Text

### Targeted Extraction

```kotlin
// Get inner text content of an element
val headingText: String? = browser.automation.getTextContent("h1.page-title")

// Get an HTML attribute value (e.g. link URL or image source)
val profileLink: String? = browser.automation.getAttribute("a#profile", "href")
val avatarSrc: String? = browser.automation.getAttribute("img.avatar", "src")
```

### Document-Level Extraction (`browser.jsBridge`)

```kotlin
// Get complete visible text of the document (document.body.innerText)
val fullText: String = browser.jsBridge.getText()

// Get full document HTML (document.documentElement.outerHTML)
val fullHtml: String = browser.jsBridge.getHtml()

// Resolve the page's favicon URL
val favicon: String? = browser.jsBridge.getFaviconUrl()
```

---

## 5. Bidirectional JavaScript Execution

The `browser.jsBridge` facade allows running custom JavaScript in the context of the page:

### Fire-and-Forget Execution

```kotlin
// Run script without waiting for a return value
browser.jsBridge.executeJavaScript("console.log('Automated action executed!')")
browser.jsBridge.executeJavaScript("window.scrollTo(0, document.body.scrollHeight)")
```

### Suspending Evaluation with Return Value

Evaluates a JavaScript expression and suspends until V8 returns the evaluated result as a String:

```kotlin
// Evaluate arbitrary JavaScript expression
val userAgent: String? = browser.jsBridge.evaluateJavaScript("navigator.userAgent")

// Query DOM metrics
val scrollHeight: String? = browser.jsBridge.evaluateJavaScript("document.body.scrollHeight")
val windowInnerWidth: String? = browser.jsBridge.evaluateJavaScript("window.innerWidth")
```

---

## 6. Clipboard & Text Editing (`browser.clipboard`)

```kotlin
// Standard clipboard operations on active focused element
browser.clipboard.selectAll()
browser.clipboard.copy()
browser.clipboard.cut()
browser.clipboard.paste()

// Undo / Redo history
browser.clipboard.undo()
browser.clipboard.redo()
```

---

## 7. Complete Real-World Script: Automated Login & Scraping

```kotlin
package com.example.automation

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

    val browser = engine.createHeadlessBrowser()

    try {
        println("1. Navigating to login portal...")
        browser.navigation.loadUrl("https://example.com/login", NavigationStage.NETWORK_IDLE)

        println("2. Waiting for login form...")
        browser.automation.waitForSelector("#login-form")

        println("3. Filling credentials...")
        browser.automation.fill("#username", "admin@example.com")
        browser.automation.type("#password", "P@ssword123", delayMs = 30L)

        println("4. Submitting...")
        browser.automation.click("button[type='submit']")

        println("5. Waiting for authenticated redirect...")
        val loggedIn = browser.automation.waitForUrl("https://example.com/dashboard")
        if (loggedIn) {
            println("Logged in successfully!")

            // Scrape user balance from dashboard
            val balance = browser.automation.getTextContent(".account-balance")
            println("Current Account Balance: $balance")
        } else {
            System.err.println("Login redirect failed or timed out.")
        }
    } finally {
        browser.close()
        engine.dispose()
    }
}
```

---

## Navigation

- [← Previous: Browser Navigation & Reactive State](navigation.md)
- [Home: Documentation Hub](../README.md)
- [Next: Interactive Dialogs & Context Menus →](dialogs-and-menus.md)
