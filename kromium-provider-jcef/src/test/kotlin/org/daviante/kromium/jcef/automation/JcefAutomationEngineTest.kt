package org.daviante.kromium.jcef.automation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.cef.browser.CefBrowser
import org.daviante.kromium.api.automation.KromiumJsBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JcefAutomationEngineTest {

    @Test
    fun testEscapeJsString() {
        assertEquals("hello", JcefAutomationEngine.escapeJsString("hello"))
        assertEquals("hello\\\'world", JcefAutomationEngine.escapeJsString("hello'world"))
        assertEquals("hello\\\"world", JcefAutomationEngine.escapeJsString("hello\"world"))
        assertEquals("line1\\nline2", JcefAutomationEngine.escapeJsString("line1\nline2"))
        assertEquals("line1\\rline2", JcefAutomationEngine.escapeJsString("line1\rline2"))
        assertEquals("line1\\tline2", JcefAutomationEngine.escapeJsString("line1\tline2"))
        assertEquals("path\\\\to\\\\file", JcefAutomationEngine.escapeJsString("path\\to\\file"))
        assertEquals("div[data-val=\\\'test\\\']", JcefAutomationEngine.escapeJsString("div[data-val='test']"))
        assertEquals("hello\\`world", JcefAutomationEngine.escapeJsString("hello`world"))
        assertEquals("line\\u2028split", JcefAutomationEngine.escapeJsString("line\u2028split"))
    }

    @Test
    fun testWaitForSelectorUsesMutationObserverPromise() = runBlocking {
        val jsBridge = mockk<KromiumJsBridge>()
        val cefBrowser = mockk<CefBrowser>(relaxed = true)

        var capturedScript = ""
        coEvery { jsBridge.evaluateJavaScript(any(), any()) } answers {
            capturedScript = firstArg()
            "true"
        }

        val engine = JcefAutomationEngine(jsBridge, cefBrowser)
        val result = engine.waitForSelector("#submit-btn", 5000L)

        assertTrue(result)
        assertTrue(capturedScript.contains("new Promise"))
        assertTrue(capturedScript.contains("MutationObserver"))
        assertTrue(capturedScript.contains("#submit-btn"))
        assertTrue(capturedScript.contains("5000"))
    }

    @Test
    fun testFillUsesPrototypeDescriptorSetter() = runBlocking {
        val jsBridge = mockk<KromiumJsBridge>()
        val cefBrowser = mockk<CefBrowser>(relaxed = true)

        val scripts = mutableListOf<String>()
        coEvery { jsBridge.evaluateJavaScript(any(), any()) } answers {
            val script = firstArg<String>()
            scripts.add(script)
            "true"
        }

        val engine = JcefAutomationEngine(jsBridge, cefBrowser)
        val result = engine.fill("input[name='query']", "search text", 5000L)

        assertTrue(result)
        // scripts[0] is waitForSelector, scripts[1] is fill
        val fillScript = scripts.last()
        assertTrue(fillScript.contains("Object.getOwnPropertyDescriptor"))
        assertTrue(fillScript.contains("descriptor.set.call(el, 'search text')"))
        assertTrue(fillScript.contains("fire('input')"))
        assertTrue(fillScript.contains("fire('change')"))
    }

    @Test
    fun testTypePerformsIncrementalStrokes() = runBlocking {
        val jsBridge = mockk<KromiumJsBridge>()
        val cefBrowser = mockk<CefBrowser>(relaxed = true)

        val scripts = mutableListOf<String>()
        coEvery { jsBridge.evaluateJavaScript(any(), any()) } answers {
            val script = firstArg<String>()
            scripts.add(script)
            "true"
        }

        val engine = JcefAutomationEngine(jsBridge, cefBrowser)
        val result = engine.type("#search", "cat", delayMs = 1L, timeoutMs = 5000L)

        assertTrue(result)
        // scripts:
        // 0: waitForSelector
        // 1: clearScript
        // 2: stroke 'c'
        // 3: stroke 'ca'
        // 4: stroke 'cat'
        // 5: final change event
        assertEquals(6, scripts.size)
        assertTrue(scripts[1].contains("descriptor.set.call(el, '')"))
        assertTrue(scripts[2].contains("descriptor.set.call(el, 'c')"))
        assertTrue(scripts[3].contains("descriptor.set.call(el, 'ca')"))
        assertTrue(scripts[4].contains("descriptor.set.call(el, 'cat')"))
        assertTrue(scripts[5].contains("new Event('change'"))
    }

    @Test
    fun testClickDispatchesPointerAndMouseEvents() = runBlocking {
        val jsBridge = mockk<KromiumJsBridge>()
        val cefBrowser = mockk<CefBrowser>(relaxed = true)

        val scripts = mutableListOf<String>()
        coEvery { jsBridge.evaluateJavaScript(any(), any()) } answers {
            val script = firstArg<String>()
            scripts.add(script)
            "true"
        }

        val engine = JcefAutomationEngine(jsBridge, cefBrowser)
        val result = engine.click("#login-btn", 5000L)

        assertTrue(result)
        val clickScript = scripts.last()
        assertTrue(clickScript.contains("scrollIntoView"))
        assertTrue(clickScript.contains("fire('pointerdown')"))
        assertTrue(clickScript.contains("fire('mousedown')"))
        assertTrue(clickScript.contains("fire('pointerup')"))
        assertTrue(clickScript.contains("fire('mouseup')"))
        assertTrue(clickScript.contains("el.click()"))
    }

    @Test
    fun testWaitForSelectorReturnsFalseOnTimeout() = runBlocking {
        val jsBridge = mockk<KromiumJsBridge>()
        val cefBrowser = mockk<CefBrowser>(relaxed = true)

        coEvery { jsBridge.evaluateJavaScript(any(), any()) } returns "false"

        val engine = JcefAutomationEngine(jsBridge, cefBrowser)
        val result = engine.waitForSelector("#missing", 1000L)

        assertFalse(result)
    }
}
