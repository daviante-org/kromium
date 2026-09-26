package org.daviante.kromium.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KromiumAssetFilterTest {

    @Test
    fun testExtractHost() {
        assertEquals("example.com", KromiumAssetFilter.extractHost("https://example.com/path"))
        assertEquals("sub.example.com", KromiumAssetFilter.extractHost("http://sub.example.com:8080/api?a=1"))
        assertEquals("ws.host.io", KromiumAssetFilter.extractHost("wss://ws.host.io:443/ws"))
        assertNull(KromiumAssetFilter.extractHost("about:blank"))
        assertNull(KromiumAssetFilter.extractHost("data:text/plain,hello"))
    }

    @Test
    fun testIsHostAllowedExactAndSubdomain() {
        val allowed = setOf("example.com", "api.github.com")

        // Exact matches
        assertTrue(KromiumAssetFilter.isHostAllowed("https://example.com/index.html", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("https://api.github.com/users", allowed))

        // Subdomain of allowed parent domain
        assertTrue(KromiumAssetFilter.isHostAllowed("https://sub.example.com/test", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("https://deep.nested.sub.example.com/test", allowed))

        // Negative matches
        assertFalse(KromiumAssetFilter.isHostAllowed("https://google.com/", allowed))
        assertFalse(KromiumAssetFilter.isHostAllowed("https://fakeexample.com/", allowed))
        assertFalse(KromiumAssetFilter.isHostAllowed("https://notexample.com/", allowed))
        assertFalse(KromiumAssetFilter.isHostAllowed("https://example.com.evil.com/", allowed))
    }

    @Test
    fun testIsHostAllowedWildcard() {
        val allowed = setOf("*.myservice.io")

        assertTrue(KromiumAssetFilter.isHostAllowed("https://myservice.io/", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("https://app.myservice.io/dashboard", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("https://auth.myservice.io/login", allowed))

        assertFalse(KromiumAssetFilter.isHostAllowed("https://other.io/", allowed))
    }

    @Test
    fun testEmptyAllowedHostsAllowsAll() {
        assertTrue(KromiumAssetFilter.isHostAllowed("https://anything.com/", emptySet()))
    }

    @Test
    fun testPseudoSchemesAlwaysAllowed() {
        val allowed = setOf("example.com")
        assertTrue(KromiumAssetFilter.isHostAllowed("about:blank", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("data:text/html,<h1>test</h1>", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("chrome://version", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("blob:https://example.com/xyz", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("file:///local/path/doc.html", allowed))
    }

    @Test
    fun testHostWithPortAndSpaces() {
        val allowed = setOf("example.com")
        assertTrue(KromiumAssetFilter.isHostAllowed("http://example.com:8080/path", allowed))
        assertTrue(KromiumAssetFilter.isHostAllowed("http://example.com/some path", allowed))
        assertFalse(KromiumAssetFilter.isHostAllowed("http://evil.com:8080/some path", allowed))
    }
}
