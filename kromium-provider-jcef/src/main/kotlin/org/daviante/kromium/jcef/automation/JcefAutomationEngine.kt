package org.daviante.kromium.jcef.automation

import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.delay
import org.cef.browser.CefBrowser
import org.daviante.kromium.api.automation.KromiumAutomation
import org.daviante.kromium.api.automation.KromiumJsBridge
import org.daviante.kromium.core.automation.KromiumEmulation
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.core.util.KromiumFutureBridge

internal class JcefAutomationEngine(
    private val jsBridge: KromiumJsBridge,
    private val cefBrowser: CefBrowser
) : KromiumAutomation {

    override fun emulateDesktopEnvironment() {
        val frame = cefBrowser.mainFrame ?: return
        frame.executeJavaScript(KromiumEmulation.SCRIPT, frame.url ?: "about:blank", 0)
    }

    override fun simulateClick(x: Int, y: Int) {
        val uiComponent = cefBrowser.uiComponent ?: return
        val mouseEventPress = MouseEvent(
            uiComponent, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
            0, x, y, 1, false, MouseEvent.BUTTON1
        )
        cefBrowser.sendMouseEvent(mouseEventPress)

        val mouseEventRelease = MouseEvent(
            uiComponent, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
            0, x, y, 1, false, MouseEvent.BUTTON1
        )
        cefBrowser.sendMouseEvent(mouseEventRelease)
    }

    private suspend fun evaluateBool(script: String, timeoutMs: Long): Boolean {
        val result = jsBridge.evaluateJavaScript(script, timeoutMs) ?: return false
        return result.trim().equals("true", ignoreCase = true) || result.trim() == "1"
    }

    override suspend fun waitForSelector(selector: String, timeoutMs: Long): Boolean {
        val safeSelector = escapeJsString(selector)
        val script = """
            new Promise((resolve) => {
                var el = document.querySelector('$safeSelector');
                if (el) return resolve(true);

                var isObserving = false;
                var observer = new MutationObserver(function() {
                    if (document.querySelector('$safeSelector')) {
                        cleanup();
                        resolve(true);
                    }
                });

                function tryObserve() {
                    if (isObserving) return;
                    var target = document.documentElement || document.body;
                    if (target) {
                        try {
                            observer.observe(target, {
                                childList: true,
                                subtree: true,
                                attributes: true
                            });
                            isObserving = true;
                        } catch (_) {}
                    }
                }

                tryObserve();

                var interval = setInterval(function() {
                    if (document.querySelector('$safeSelector')) {
                        cleanup();
                        resolve(true);
                    } else if (!isObserving) {
                        tryObserve();
                    }
                }, 50);

                function cleanup() {
                    try { observer.disconnect(); } catch (_) {}
                    clearInterval(interval);
                }

                setTimeout(function() {
                    cleanup();
                    resolve(false);
                }, $timeoutMs);
            })
        """.trimIndent()

        val evaluationTimeout = if (timeoutMs > 0) timeoutMs + 2000L else null
        val result = jsBridge.evaluateJavaScript(script, timeoutMs = evaluationTimeout)
        return result?.trim()?.equals("true", ignoreCase = true) == true
    }

    override suspend fun waitForUrl(pattern: String, isRegex: Boolean, timeoutMs: Long): Boolean {
        val regex = if (isRegex) Regex(pattern) else null
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentUrl = cefBrowser.url
            if (currentUrl != null) {
                val matched = if (regex != null) {
                    regex.containsMatchIn(currentUrl)
                } else {
                    currentUrl.contains(pattern, ignoreCase = true)
                }
                if (matched) return true
            }
            delay(50)
        }

        return false
    }

    override suspend fun click(selector: String, timeoutMs: Long): Boolean {
        val found = waitForSelector(selector, timeoutMs)
        if (!found) {
            KromiumLogger.w(TAG, "click: timeout waiting for selector '$selector'")
            return false
        }

        val safeSelector = escapeJsString(selector)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                if (!el) return false;
                if (el.disabled) return false;

                try {
                    el.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'center' });
                } catch (_) {
                    try { el.scrollIntoView(); } catch (__) {}
                }

                function fire(type) {
                    var ev;
                    if (typeof MouseEvent === 'function') {
                        ev = new MouseEvent(type, { bubbles: true, cancelable: true, view: window });
                    } else {
                        ev = document.createEvent('MouseEvents');
                        ev.initEvent(type, true, true);
                    }
                    el.dispatchEvent(ev);
                }

                fire('pointerdown');
                fire('mousedown');
                fire('pointerup');
                fire('mouseup');
                el.click();
                return true;
            })()
        """.trimIndent()

        return evaluateBool(script, 5000L)
    }

    override suspend fun fill(selector: String, value: String, timeoutMs: Long): Boolean {
        val found = waitForSelector(selector, timeoutMs)
        if (!found) {
            KromiumLogger.w(TAG, "fill: timeout waiting for selector '$selector'")
            return false
        }

        val safeSelector = escapeJsString(selector)
        val safeValue = escapeJsString(value)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                if (!el) return false;

                el.focus();
                var proto = Object.getPrototypeOf(el);
                var descriptor = Object.getOwnPropertyDescriptor(proto, 'value') ||
                                 Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value') ||
                                 Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');

                if (descriptor && descriptor.set) {
                    descriptor.set.call(el, '$safeValue');
                } else {
                    el.value = '$safeValue';
                }

                function fire(type) {
                    var ev;
                    if (typeof Event === 'function') {
                        ev = new Event(type, { bubbles: true, cancelable: true });
                    } else {
                        ev = document.createEvent('Event');
                        ev.initEvent(type, true, true);
                    }
                    el.dispatchEvent(ev);
                }

                fire('input');
                fire('change');
                return true;
            })()
        """.trimIndent()

        return evaluateBool(script, 5000L)
    }

    override suspend fun type(selector: String, text: String, delayMs: Long, timeoutMs: Long): Boolean {
        if (!waitForSelector(selector, timeoutMs)) return false

        val safeSelector = escapeJsString(selector)

        // Clear and focus input once at the beginning
        val clearScript = """
            (function() {
                var el = document.querySelector('$safeSelector');
                if (!el) return false;
                el.focus();
                var proto = Object.getPrototypeOf(el);
                var descriptor = Object.getOwnPropertyDescriptor(proto, 'value') ||
                                 Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value') ||
                                 Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                if (descriptor && descriptor.set) {
                    descriptor.set.call(el, '');
                } else {
                    el.value = '';
                }
                el.dispatchEvent(new Event('input', { bubbles: true }));
                return true;
            })()
        """.trimIndent()
        jsBridge.evaluateJavaScript(clearScript)

        var accumulated = ""
        for (char in text) {
            accumulated += char
            val safeAccumulated = escapeJsString(accumulated)
            val strokeScript = """
                (function() {
                    var el = document.querySelector('$safeSelector');
                    if (!el) return false;
                    var proto = Object.getPrototypeOf(el);
                    var descriptor = Object.getOwnPropertyDescriptor(proto, 'value') ||
                                     Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value') ||
                                     Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                    if (descriptor && descriptor.set) {
                        descriptor.set.call(el, '$safeAccumulated');
                    } else {
                        el.value = '$safeAccumulated';
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    return true;
                })()
            """.trimIndent()
            jsBridge.evaluateJavaScript(strokeScript)
            if (delayMs > 0) delay(delayMs)
        }

        // Dispatch final change event when typing finishes
        val finalScript = """
            (function() {
                var el = document.querySelector('$safeSelector');
                if (el) el.dispatchEvent(new Event('change', { bubbles: true }));
            })()
        """.trimIndent()
        jsBridge.evaluateJavaScript(finalScript)
        return true
    }

    override suspend fun selectOption(selector: String, value: String, timeoutMs: Long): Boolean {
        val found = waitForSelector(selector, timeoutMs)
        if (!found) return false

        val safeSelector = escapeJsString(selector)
        val safeValue = escapeJsString(value)
        val script = """
            (function() {
                var select = document.querySelector('$safeSelector');
                if (select) {
                    select.value = '$safeValue';
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                    return true;
                }
                return false;
            })();
        """.trimIndent()
        return evaluateBool(script, 5000L)
    }

    override suspend fun getTextContent(selector: String, timeoutMs: Long): String? {
        val found = waitForSelector(selector, timeoutMs)
        if (!found) return null

        val safeSelector = escapeJsString(selector)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                return el ? el.textContent : null;
            })();
        """.trimIndent()
        return jsBridge.evaluateJavaScript(script, 5000L)
    }

    override suspend fun getAttribute(selector: String, attribute: String, timeoutMs: Long): String? {
        val found = waitForSelector(selector, timeoutMs)
        if (!found) return null

        val safeSelector = escapeJsString(selector)
        val safeAttr = escapeJsString(attribute)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                return el ? el.getAttribute('$safeAttr') : null;
            })();
        """.trimIndent()
        return jsBridge.evaluateJavaScript(script, 5000L)
    }

    override suspend fun isVisible(selector: String): Boolean {
        val safeSelector = escapeJsString(selector)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                if (!el) return false;
                var style = window.getComputedStyle(el);
                return style && style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
            })();
        """.trimIndent()
        return evaluateBool(script, 5000L)
    }

    override suspend fun isChecked(selector: String): Boolean {
        val safeSelector = escapeJsString(selector)
        val script = """
            (function() {
                var el = document.querySelector('$safeSelector');
                return el ? el.checked === true : false;
            })();
        """.trimIndent()
        return evaluateBool(script, 5000L)
    }

    override suspend fun count(selector: String): Int {
        val safeSelector = escapeJsString(selector)
        val script = """
            (function() {
                return document.querySelectorAll('$safeSelector').length.toString();
            })();
        """.trimIndent()
        val res = jsBridge.evaluateJavaScript(script, 5000L)
        return res?.toIntOrNull() ?: 0
    }

    override fun waitForSelectorAsync(selector: String, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { waitForSelector(selector, timeoutMs) }

    override fun waitForUrlAsync(pattern: String, isRegex: Boolean, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { waitForUrl(pattern, isRegex, timeoutMs) }

    override fun clickAsync(selector: String, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { click(selector, timeoutMs) }

    override fun fillAsync(selector: String, value: String, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { fill(selector, value, timeoutMs) }

    override fun typeAsync(selector: String, text: String, delayMs: Long, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { type(selector, text, delayMs, timeoutMs) }

    override fun selectOptionAsync(selector: String, value: String, timeoutMs: Long): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { selectOption(selector, value, timeoutMs) }

    override fun getTextContentAsync(selector: String, timeoutMs: Long): CompletableFuture<String?> =
        KromiumFutureBridge.toCompletableFuture { getTextContent(selector, timeoutMs) }

    override fun getAttributeAsync(selector: String, attribute: String, timeoutMs: Long): CompletableFuture<String?> =
        KromiumFutureBridge.toCompletableFuture { getAttribute(selector, attribute, timeoutMs) }

    override fun isVisibleAsync(selector: String): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { isVisible(selector) }

    override fun isCheckedAsync(selector: String): CompletableFuture<Boolean> =
        KromiumFutureBridge.toCompletableFuture { isChecked(selector) }

    override fun countAsync(selector: String): CompletableFuture<Int> =
        KromiumFutureBridge.toCompletableFuture { count(selector) }

    companion object {
        private const val TAG = "JcefAutomationEngine"

        /**
         * Safely escapes strings for embedding into JavaScript string literals.
         */
        fun escapeJsString(value: String): String {
            val sb = StringBuilder(value.length + 16)
            for (ch in value) {
                when (ch) {
                    '\\' -> sb.append("\\\\")
                    '\'' -> sb.append("\\'")
                    '"' -> sb.append("\\\"")
                    '`' -> sb.append("\\`")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    '\u2028' -> sb.append("\\u2028")
                    '\u2029' -> sb.append("\\u2029")
                    else -> {
                        if (ch < ' ') {
                            sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                        } else {
                            sb.append(ch)
                        }
                    }
                }
            }
            return sb.toString()
        }
    }
}
