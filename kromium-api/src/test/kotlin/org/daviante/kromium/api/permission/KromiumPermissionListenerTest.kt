package org.daviante.kromium.api.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KromiumPermissionListenerTest {

    @Test
    fun testPermissionTypeBitmaskConversions() {
        assertEquals(emptySet(), KromiumPermissionType.fromFlags(0))
        assertEquals(setOf(KromiumPermissionType.AUDIO_CAPTURE), KromiumPermissionType.fromFlags(1))
        assertEquals(setOf(KromiumPermissionType.VIDEO_CAPTURE), KromiumPermissionType.fromFlags(2))
        assertEquals(
            setOf(KromiumPermissionType.AUDIO_CAPTURE, KromiumPermissionType.VIDEO_CAPTURE),
            KromiumPermissionType.fromFlags(3)
        )
        assertEquals(setOf(KromiumPermissionType.DESKTOP_AUDIO), KromiumPermissionType.fromFlags(4))
        assertEquals(setOf(KromiumPermissionType.DESKTOP_VIDEO), KromiumPermissionType.fromFlags(8))
        assertEquals(
            setOf(
                KromiumPermissionType.AUDIO_CAPTURE,
                KromiumPermissionType.VIDEO_CAPTURE,
                KromiumPermissionType.DESKTOP_AUDIO,
                KromiumPermissionType.DESKTOP_VIDEO
            ),
            KromiumPermissionType.fromFlags(15)
        )

        assertEquals(0, KromiumPermissionType.toFlags(emptyList()))
        assertEquals(1, KromiumPermissionType.toFlags(listOf(KromiumPermissionType.AUDIO_CAPTURE)))
        assertEquals(2, KromiumPermissionType.toFlags(listOf(KromiumPermissionType.VIDEO_CAPTURE)))
        assertEquals(
            3,
            KromiumPermissionType.toFlags(listOf(KromiumPermissionType.AUDIO_CAPTURE, KromiumPermissionType.VIDEO_CAPTURE))
        )
        assertEquals(
            15,
            KromiumPermissionType.toFlags(
                listOf(
                    KromiumPermissionType.AUDIO_CAPTURE,
                    KromiumPermissionType.VIDEO_CAPTURE,
                    KromiumPermissionType.DESKTOP_AUDIO,
                    KromiumPermissionType.DESKTOP_VIDEO
                )
            )
        )
    }

    @Test
    fun testPermissionRequestParsesOriginsAndFlags() {
        val request = KromiumPermissionRequest.from("https://meet.google.com/abc-xyz", 3)
        assertEquals("https://meet.google.com/abc-xyz", request.url)
        assertEquals("https://meet.google.com", request.origin)
        assertEquals(3, request.rawFlags)
        assertTrue(request.hasAudio())
        assertTrue(request.hasVideo())
        assertFalse(request.hasScreenShare())
        assertFalse(request.hasDesktopAudio())
        assertTrue(request.contains(KromiumPermissionType.AUDIO_CAPTURE))
        assertTrue(request.contains(KromiumPermissionType.VIDEO_CAPTURE))

        val screenShareReq = KromiumPermissionRequest.from("https://zoom.us:8443/j/123", 12)
        assertEquals("https://zoom.us:8443", screenShareReq.origin)
        assertFalse(screenShareReq.hasAudio())
        assertFalse(screenShareReq.hasVideo())
        assertTrue(screenShareReq.hasDesktopAudio())
        assertTrue(screenShareReq.hasScreenShare())

        val customAppReq = KromiumPermissionRequest.from("app://kromium-desktop/media", 1)
        assertEquals("app://kromium-desktop", customAppReq.origin)
    }

    @Test
    fun testPermissionDecisionFactories() {
        assertEquals(KromiumPermissionDecision.Grant(null), KromiumPermissionDecision.GRANT)
        assertEquals(KromiumPermissionDecision.Deny, KromiumPermissionDecision.DENY)

        val selectiveGrant = KromiumPermissionDecision.grant(KromiumPermissionType.AUDIO_CAPTURE)
        assertEquals(setOf(KromiumPermissionType.AUDIO_CAPTURE), selectiveGrant.allowedTypes)
    }

    @Test
    fun testPermissionListenerPresets() {
        val req = KromiumPermissionRequest.from("https://example.com/stream", 3)

        val grantAll = KromiumPermissionListener.grantAll()
        assertEquals(KromiumPermissionDecision.GRANT, grantAll.onRequestPermission(req))

        val denyAll = KromiumPermissionListener.denyAll()
        assertEquals(KromiumPermissionDecision.DENY, denyAll.onRequestPermission(req))

        val forOrigins = KromiumPermissionListener.forOrigins("https://example.com")
        assertEquals(KromiumPermissionDecision.GRANT, forOrigins.onRequestPermission(req))

        val otherReq = KromiumPermissionRequest.from("https://other.com/call", 3)
        assertEquals(KromiumPermissionDecision.DENY, forOrigins.onRequestPermission(otherReq))
    }
}
