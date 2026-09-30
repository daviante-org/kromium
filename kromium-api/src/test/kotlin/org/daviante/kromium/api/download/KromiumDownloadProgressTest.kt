package org.daviante.kromium.api.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KromiumDownloadProgressTest {

    @Test
    fun testPercentageCalculation() {
        val p1 = KromiumDownloadProgress(50L, 100L, 0.5f)
        assertEquals(50, p1.percentage, "Percentage should correctly scale fraction to 1-100")

        val p2 = KromiumDownloadProgress(100L, 100L, 1.0f)
        assertEquals(100, p2.percentage, "Completed download should have 100% progress")

        val pZero = KromiumDownloadProgress(0L, 100L, 0.0f)
        assertEquals(0, pZero.percentage, "Zero bytes downloaded should be 0%")
    }

    @Test
    fun testInitialState() {
        val pInitial = KromiumDownloadProgress.Initial
        assertEquals(0, pInitial.percentage, "Initial percentage should be 0")
        assertNull(pInitial.totalBytes, "Initial total bytes should be null before size is known")
        assertEquals(0L, pInitial.bytesRead, "Initial downloaded bytes should be 0")
    }

    @Test
    fun testUnknownTotalSize() {
        val pUnknown = KromiumDownloadProgress(500L, null, 0.0f)
        assertEquals(500L, pUnknown.bytesRead)
        assertNull(pUnknown.totalBytes)
        assertEquals(0, pUnknown.percentage)
    }
}
