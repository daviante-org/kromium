package org.daviante.kromium.jcef.permission

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.permission.KromiumPermissionDecision
import org.daviante.kromium.api.permission.KromiumPermissionListener
import org.daviante.kromium.jcef.core.JcefKromiumClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cef.callback.CefMediaAccessCallback
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcefPermissionAdapterTest {

    @Test
    fun testPermissionGrantAndSessionCaching() {
        val permissionCache = ConcurrentHashMap<String, Int>()
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.rememberPermissions } returns true
        every { client.getPermissionCache() } returns permissionCache

        var invocations = 0
        val listener = KromiumPermissionListener {
            invocations++
            KromiumPermissionDecision.GRANT
        }
        every { client.permissionListener } returns listener

        val adapter = JcefPermissionAdapter(client, KromiumClientConfig.default())
        val callback1 = mockk<CefMediaAccessCallback>(relaxed = true)

        // 1. First invocation for origin
        val handled1 = adapter.onRequestMediaAccessPermission(
            null, null, "https://meet.google.com/room1", 3, callback1
        )
        assertTrue(handled1)
        assertEquals(1, invocations)
        verify { callback1.Continue(3) }
        assertEquals(3, permissionCache["https://meet.google.com"])

        // 2. Second invocation for same origin should hit cache without calling listener
        val callback2 = mockk<CefMediaAccessCallback>(relaxed = true)
        val handled2 = adapter.onRequestMediaAccessPermission(
            null, null, "https://meet.google.com/room2", 3, callback2
        )
        assertTrue(handled2)
        assertEquals(1, invocations, "Listener should not be called again when cached")
        verify { callback2.Continue(3) }

        // 3. Clear permission cache
        permissionCache.clear()

        // 4. Third invocation after clear should call listener again
        val callback3 = mockk<CefMediaAccessCallback>(relaxed = true)
        val handled3 = adapter.onRequestMediaAccessPermission(
            null, null, "https://meet.google.com/room3", 3, callback3
        )
        assertTrue(handled3)
        assertEquals(2, invocations, "Listener should be called again after clearing cache")
        verify { callback3.Continue(3) }
    }

    @Test
    fun testPermissionDeny() {
        val client = mockk<JcefKromiumClient>(relaxed = true)
        every { client.rememberPermissions } returns false
        every { client.permissionListener } returns KromiumPermissionListener.denyAll()

        val adapter = JcefPermissionAdapter(client, KromiumClientConfig.default())
        val callback = mockk<CefMediaAccessCallback>(relaxed = true)

        val handled = adapter.onRequestMediaAccessPermission(
            null, null, "https://untrusted.com", 3, callback
        )
        assertTrue(handled)
        verify { callback.Cancel() }
    }
}
