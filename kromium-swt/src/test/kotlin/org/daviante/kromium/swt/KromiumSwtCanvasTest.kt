package org.daviante.kromium.swt

import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.PaletteData
import java.awt.Rectangle
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KromiumSwtCanvasTest {

    @Test
    fun testPaletteDataColorMapping() {
        val palette = PaletteData(0x0000FF00, 0x00FF0000, 0xFF000000.toInt())
        // BGRA bytes for pure Red: Blue=0, Green=0, Red=255, Alpha=255
        val redPixelBytes = byteArrayOf(0, 0, 255.toByte(), 255.toByte())
        val imgData = ImageData(1, 1, 32, palette, 4, redPixelBytes)
        val pixel = imgData.getPixel(0, 0)

        val red = (pixel and palette.redMask) ushr 8
        val green = (pixel and palette.greenMask) ushr 16
        val blue = (pixel and palette.blueMask) ushr 24

        assertEquals(255, red)
        assertEquals(0, green)
        assertEquals(0, blue)
    }

    @Test
    fun testPaletteDataBlueMapping() {
        val palette = PaletteData(0x0000FF00, 0x00FF0000, 0xFF000000.toInt())
        // BGRA bytes for pure Blue: Blue=255, Green=0, Red=0, Alpha=255
        val bluePixelBytes = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
        val imgData = ImageData(1, 1, 32, palette, 4, bluePixelBytes)
        val pixel = imgData.getPixel(0, 0)

        val red = (pixel and palette.redMask) ushr 8
        val green = (pixel and palette.greenMask) ushr 16
        val blue = (pixel and palette.blueMask) ushr 24

        assertEquals(0, red)
        assertEquals(0, green)
        assertEquals(255, blue)
    }

    @Test
    fun testOsrComponentInterface() {
        val isOsr = JcefOsrComponent::class.java.isAssignableFrom(KromiumSwtCanvas::class.java)
        assertTrue(isOsr)
    }

    @Test
    fun testSwtCanvasRegistrationAndFactory() {
        KromiumSwtCanvas.registerFactory()
        assertTrue(JcefOsrPanelFactory.hasFactory(KromiumUiFramework.SWT))
    }

    @Test
    fun testSwtOsrBuffer() {
        val buffer = SwtOsrBuffer()
        val byteBuf = ByteBuffer.allocateDirect(16)
        buffer.onPaint(byteBuf, 2, 2, false)
        assertEquals(byteBuf, buffer.lastBuffer)
        assertEquals(2, buffer.lastWidth)
        assertEquals(2, buffer.lastHeight)
        assertEquals(false, buffer.isLastPopup)

        val rect = Rectangle(5, 10, 20, 30)
        buffer.setPopupBounds(rect)
        assertEquals(rect, buffer.popupBounds)
        buffer.setPopupVisible(true)
        assertEquals(true, buffer.isPopupVisible)
    }

    @Test
    fun testToSwtCursorMapping() {
        assertEquals(org.eclipse.swt.SWT.CURSOR_ARROW, KromiumSwtCanvas.toSwtCursor(0))
        assertEquals(org.eclipse.swt.SWT.CURSOR_CROSS, KromiumSwtCanvas.toSwtCursor(1))
        assertEquals(org.eclipse.swt.SWT.CURSOR_HAND, KromiumSwtCanvas.toSwtCursor(2))
        assertEquals(org.eclipse.swt.SWT.CURSOR_IBEAM, KromiumSwtCanvas.toSwtCursor(3)) // I-beam for inputs
        assertEquals(org.eclipse.swt.SWT.CURSOR_WAIT, KromiumSwtCanvas.toSwtCursor(4))
        assertEquals(org.eclipse.swt.SWT.CURSOR_IBEAM, KromiumSwtCanvas.toSwtCursor(30)) // Vertical text
        assertEquals(org.eclipse.swt.SWT.CURSOR_HAND, KromiumSwtCanvas.toSwtCursor(39)) // Grab
    }

    @Test
    fun testCefCursorToAwtMapping() {
        val textCursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR)
        val defaultCursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR)
        val handCursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)

        assertEquals(defaultCursor, org.cef.browser.CefBrowserOsr.mapCefCursorToAwt(0))
        assertEquals(handCursor, org.cef.browser.CefBrowserOsr.mapCefCursorToAwt(2))
        assertEquals(textCursor, org.cef.browser.CefBrowserOsr.mapCefCursorToAwt(3)) // I-beam
        assertEquals(textCursor, org.cef.browser.CefBrowserOsr.mapCefCursorToAwt(30))
    }
}

