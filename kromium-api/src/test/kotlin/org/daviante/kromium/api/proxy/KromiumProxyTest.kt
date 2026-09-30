package org.daviante.kromium.api.proxy

import org.daviante.kromium.api.config.KromiumBrowserConfig
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.error.KromiumException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KromiumProxyTest {

    @Test
    fun testSystemProxyStrategy() {
        val proxy = KromiumProxy.System
        assertTrue(proxy.toCommandLineArgs().isEmpty())
        assertEquals(mapOf("mode" to "system"), proxy.toPreferenceMap())
        assertNull(proxy.getCredentials("proxy.internal", 8080))
    }

    @Test
    fun testDirectProxyStrategy() {
        val proxy = KromiumProxy.Direct
        assertEquals(listOf("--no-proxy-server"), proxy.toCommandLineArgs())
        assertEquals(mapOf("mode" to "direct"), proxy.toPreferenceMap())
    }

    @Test
    fun testAutoDetectStrategy() {
        val proxy = KromiumProxy.AutoDetect
        assertEquals(listOf("--proxy-auto-detect"), proxy.toCommandLineArgs())
        assertEquals(mapOf("mode" to "auto_detect"), proxy.toPreferenceMap())
    }

    @Test
    fun testPacProxyStrategy() {
        val pac = KromiumProxy.Pac("http://pac.corp.internal/wpad.dat")
        assertEquals(listOf("--proxy-pac-url=http://pac.corp.internal/wpad.dat"), pac.toCommandLineArgs())
        assertEquals(
            mapOf("mode" to "pac_script", "pac_url" to "http://pac.corp.internal/wpad.dat"),
            pac.toPreferenceMap()
        )

        assertFailsWith<KromiumException.InvalidConfig> {
            KromiumProxy.Pac("").validate()
        }
    }

    @Test
    fun testHttpProxyWithBypassListAndCredentials() {
        val proxy = KromiumProxy.Http(
            host = "proxy.corp.com",
            port = 8080,
            username = "domain\\user",
            password = "SecurePassword123",
            isSecure = false,
            bypassList = listOf("<local>", "127.0.0.1", "*.internal.corp")
        )

        val args = proxy.toCommandLineArgs()
        assertTrue(args.contains("--proxy-server=http://proxy.corp.com:8080"))
        assertTrue(args.contains("--proxy-bypass-list=<local>;127.0.0.1;*.internal.corp"))

        val pref = proxy.toPreferenceMap()
        assertEquals("fixed_servers", pref["mode"])
        assertEquals("http://proxy.corp.com:8080", pref["server"])
        assertEquals("<local>;127.0.0.1;*.internal.corp", pref["bypass_list"])

        val creds = proxy.getCredentials("proxy.corp.com", 8080)
        assertNotNull(creds)
        assertEquals("domain\\user", creds.first)
        assertEquals("SecurePassword123", creds.second)

        // Verifies port-specific and host-specific credential filtering
        assertNull(proxy.getCredentials("other.host.com", 8080))
        assertNull(proxy.getCredentials("proxy.corp.com", 9090))
    }

    @Test
    fun testSecureHttpsProxyTunnel() {
        val proxy = KromiumProxy.Http(
            host = "secure-egress.corp.com",
            port = 8443,
            isSecure = true
        )

        val args = proxy.toCommandLineArgs()
        assertTrue(args.contains("--proxy-server=https://secure-egress.corp.com:8443"))

        val pref = proxy.toPreferenceMap()
        assertEquals("fixed_servers", pref["mode"])
        assertEquals("https://secure-egress.corp.com:8443", pref["server"])
    }

    @Test
    fun testSocks5ProxyWithRemoteDnsAndBypass() {
        val socks5Proxy = KromiumProxy.Socks5(
            host = "10.0.0.1",
            port = 1080,
            username = "socksuser",
            password = "sockspassword",
            remoteDns = true,
            bypassList = listOf("localhost", "127.0.0.1")
        )

        val args = socks5Proxy.toCommandLineArgs()
        assertTrue(args.contains("--proxy-server=socks5://10.0.0.1:1080"))
        assertTrue(args.contains("--proxy-bypass-list=localhost;127.0.0.1"))

        val pref = socks5Proxy.toPreferenceMap()
        assertEquals("fixed_servers", pref["mode"])
        assertEquals("socks5://10.0.0.1:1080", pref["server"])
        assertEquals("localhost;127.0.0.1", pref["bypass_list"])

        val creds = socks5Proxy.getCredentials("10.0.0.1", 1080)
        assertNotNull(creds)
        assertEquals("socksuser", creds.first)
        assertEquals("sockspassword", creds.second)

        // When remoteDns is false, scheme becomes socks4
        val socks4Proxy = KromiumProxy.Socks5(
            host = "10.0.0.1",
            port = 1080,
            remoteDns = false
        )
        assertTrue(socks4Proxy.toCommandLineArgs().contains("--proxy-server=socks4://10.0.0.1:1080"))
    }

    @Test
    fun testMultiProtocolProxyStrategyWithStrings() {
        val proxy = KromiumProxy.MultiProtocol(
            http = "http://user:pass@http-proxy.corp:8080",
            https = "https://https-proxy.corp:8443",
            socks = "socks5://socks-proxy.corp:1080",
            bypassList = listOf("<local>", "*.corp")
        )

        val args = proxy.toCommandLineArgs()
        assertTrue(args.contains("--proxy-server=http=http://http-proxy.corp:8080;https=https://https-proxy.corp:8443;socks=socks5://socks-proxy.corp:1080"))
        assertTrue(args.contains("--proxy-bypass-list=<local>;*.corp"))

        val pref = proxy.toPreferenceMap()
        assertEquals("fixed_servers", pref["mode"])
        assertEquals("http=http://http-proxy.corp:8080;https=https://https-proxy.corp:8443;socks=socks5://socks-proxy.corp:1080", pref["server"])
        assertEquals("<local>;*.corp", pref["bypass_list"])

        val creds = proxy.getCredentials("http-proxy.corp", 8080)
        assertNotNull(creds)
        assertEquals("user", creds.first)
        assertEquals("pass", creds.second)
    }

    @Test
    fun testMultiProtocolProxyStrategyWithTypedEndpoints() {
        val httpEndpoint = KromiumProxy.Http("http-proxy.corp", 8080, "typedUser", "typedPass")
        val socksEndpoint = KromiumProxy.Socks5("socks-proxy.corp", 1080)

        val proxy = KromiumProxy.MultiProtocol(
            http = httpEndpoint,
            socks = socksEndpoint,
            bypassList = listOf("internal.net")
        )

        val args = proxy.toCommandLineArgs()
        assertTrue(args.contains("--proxy-server=http=http://http-proxy.corp:8080;socks=socks5://socks-proxy.corp:1080"))
        assertTrue(args.contains("--proxy-bypass-list=internal.net"))

        val creds = proxy.getCredentials("http-proxy.corp", 8080)
        assertNotNull(creds)
        assertEquals("typedUser", creds.first)
        assertEquals("typedPass", creds.second)
    }

    @Test
    fun testInvalidProxyValidation() {
        assertFailsWith<KromiumException.InvalidConfig> {
            KromiumProxy.Http("", 8080).validate()
        }
        assertFailsWith<KromiumException.InvalidConfig> {
            KromiumProxy.Http("proxy.com", 70000).validate()
        }
        assertFailsWith<KromiumException.InvalidConfig> {
            KromiumProxy.Socks5("proxy.com", -1).validate()
        }
        assertFailsWith<KromiumException.InvalidConfig> {
            KromiumProxy.MultiProtocol().validate()
        }
    }

    @Test
    fun testStaticFactoryHelpers() {
        assertEquals(KromiumProxy.System, KromiumProxy.system())
        assertEquals(KromiumProxy.Direct, KromiumProxy.direct())
        assertEquals(KromiumProxy.AutoDetect, KromiumProxy.autoDetect())
        assertTrue(KromiumProxy.http("proxy.internal", 8080) is KromiumProxy.Http)
        assertTrue(KromiumProxy.https("secure.internal", 8443) is KromiumProxy.Http)
        assertTrue(KromiumProxy.socks5("127.0.0.1", 1080) is KromiumProxy.Socks5)
        assertTrue(KromiumProxy.multiProtocol(http = "http://proxy.corp:8080") is KromiumProxy.MultiProtocol)
    }

    @Test
    fun testBrowserAndClientConfigProxyIntegration() {
        val testProxy = KromiumProxy.http("proxy.corp", 8080, bypassList = listOf("localhost"))

        val browserConfig = KromiumBrowserConfig.builder()
            .proxy(testProxy)
            .build()
        assertEquals(testProxy, browserConfig.proxy)

        val fullConfig = KromiumConfig.builder()
            .browserConfig(browserConfig)
            .build()
        assertEquals(testProxy, fullConfig.browserConfig.proxy)

        val clientConfig = KromiumClientConfig.builder()
            .proxy(testProxy)
            .build()
        assertEquals(testProxy, clientConfig.proxy)
    }
}
