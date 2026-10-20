package org.daviante.kromium.api.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KromiumNavigationTest {

    private class TestNavigation : KromiumNavigation {
        var lastLoadedHtml: String? = null
        var lastLoadedHtmlUrl: String? = null
        var lastLoadedUrl: String? = null
        var backCalled = false
        var forwardCalled = false
        var reloadCalled = false
        var stopCalled = false

        private val _state = MutableStateFlow(KromiumNavigationState())
        override val navigationState: StateFlow<KromiumNavigationState> = _state

        override suspend fun loadUrl(url: String, waitUntil: NavigationStage, timeoutMs: Long): Boolean {
            lastLoadedUrl = url
            return true
        }

        override fun loadUrl(url: String) {
            lastLoadedUrl = url
        }

        override fun loadUrlAsync(url: String, waitUntil: NavigationStage, timeoutMs: Long): CompletableFuture<Boolean> {
            lastLoadedUrl = url
            return CompletableFuture.completedFuture(true)
        }

        override suspend fun waitForNavigation(stage: NavigationStage, timeoutMs: Long): Boolean = true

        override fun waitForNavigationAsync(stage: NavigationStage, timeoutMs: Long): CompletableFuture<Boolean> =
            CompletableFuture.completedFuture(true)

        override suspend fun waitForNetworkIdle(idleTimeMs: Long, maxTimeoutMs: Long, pollIntervalMs: Long): Boolean = true

        override fun waitForNetworkIdleAsync(idleTimeMs: Long, maxTimeoutMs: Long, pollIntervalMs: Long): CompletableFuture<Boolean> =
            CompletableFuture.completedFuture(true)

        override fun loadHtml(html: String, url: String) {
            lastLoadedHtml = html
            lastLoadedHtmlUrl = url
        }

        override fun goBack() { backCalled = true }
        override fun goForward() { forwardCalled = true }
        override fun canGoBack(): Boolean = true
        override fun canGoForward(): Boolean = false
        override fun reload(ignoreCache: Boolean) { reloadCalled = true }
        override fun stopLoad() { stopCalled = true }
    }

    @Test
    fun testLoadHtmlWithDefaultUrl() {
        val nav: KromiumNavigation = TestNavigation()
        val testHtml = "<h1>Hello Kromium</h1>"
        nav.loadHtml(testHtml)

        val testNav = nav as TestNavigation
        assertEquals(testHtml, testNav.lastLoadedHtml)
        assertEquals("https://kromium.local/", testNav.lastLoadedHtmlUrl)
    }

    @Test
    fun testLoadHtmlWithCustomUrl() {
        val nav: KromiumNavigation = TestNavigation()
        val testHtml = "<p>Custom Base URL</p>"
        val customUrl = "https://example.com/app"
        nav.loadHtml(testHtml, customUrl)

        val testNav = nav as TestNavigation
        assertEquals(testHtml, testNav.lastLoadedHtml)
        assertEquals(customUrl, testNav.lastLoadedHtmlUrl)
    }

    @Test
    fun testNavigationControls() {
        val nav: KromiumNavigation = TestNavigation()
        nav.goBack()
        nav.goForward()
        nav.reload()
        nav.stopLoad()
        nav.loadUrl("https://example.com")

        val testNav = nav as TestNavigation
        assertTrue(testNav.backCalled)
        assertTrue(testNav.forwardCalled)
        assertTrue(testNav.reloadCalled)
        assertTrue(testNav.stopCalled)
        assertTrue(nav.canGoBack())
        assertEquals(false, nav.canGoForward())
        assertEquals("https://example.com", testNav.lastLoadedUrl)
    }
}
