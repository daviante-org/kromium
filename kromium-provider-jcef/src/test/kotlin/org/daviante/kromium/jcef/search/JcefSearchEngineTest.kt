package org.daviante.kromium.jcef.search

import io.mockk.mockk
import io.mockk.verify
import org.cef.browser.CefBrowser
import kotlin.test.Test

class JcefSearchEngineTest {

    @Test
    fun testFindDelegation() {
        val cefBrowser = mockk<CefBrowser>(relaxed = true)
        val searchEngine = JcefSearchEngine(cefBrowser)

        searchEngine.find("hello", forward = true, matchCase = false, findNext = false)
        verify(exactly = 1) { cefBrowser.find("hello", true, false, false) }

        searchEngine.find("world", forward = false, matchCase = true, findNext = true)
        verify(exactly = 1) { cefBrowser.find("world", false, true, true) }
    }

    @Test
    fun testStopFindingDelegation() {
        val cefBrowser = mockk<CefBrowser>(relaxed = true)
        val searchEngine = JcefSearchEngine(cefBrowser)

        searchEngine.stopFinding(clearSelection = true)
        verify(exactly = 1) { cefBrowser.stopFinding(true) }

        searchEngine.stopFinding(clearSelection = false)
        verify(exactly = 1) { cefBrowser.stopFinding(false) }
    }
}
