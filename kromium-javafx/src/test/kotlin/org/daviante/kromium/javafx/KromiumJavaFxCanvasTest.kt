package org.daviante.kromium.javafx

import io.mockk.every
import io.mockk.mockk
import javafx.application.Platform
import org.cef.browser.CefBrowser
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import java.awt.Rectangle
import java.nio.ByteBuffer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KromiumJavaFxCanvasTest {

    companion object {
        private var isFxAvailable: Boolean = false
        private var initAttempted: Boolean = false

        fun ensureJavaFx(): Boolean {
            if (!initAttempted) {
                initAttempted = true
                try {
                    Platform.startup {}
                    isFxAvailable = true
                } catch (_: IllegalStateException) {
                    isFxAvailable = true
                } catch (e: Throwable) {
                    System.err.println("JavaFX toolkit unavailable in this environment: ${e.message}")
                    isFxAvailable = false
                }
            }
            return isFxAvailable
        }
    }

    @BeforeTest
    fun setupFx() {
        org.junit.Assume.assumeTrue("JavaFX requires a graphical display environment", ensureJavaFx())
    }

    @Test
    fun testCanvasInstantiationWithoutBrowser() {
        val canvas = KromiumJavaFxCanvas()
        assertNotNull(canvas)
        assertNull(canvas.activeBrowser)
        assertTrue(JcefOsrComponent::class.java.isAssignableFrom(KromiumJavaFxCanvas::class.java))
        assertEquals(canvas, canvas.getUiObject())
    }

    @Test
    fun testCanvasInstantiationWithBrowser() {
        val mockBrowser = mockk<KromiumBrowser>(relaxed = true)
        every { mockBrowser.view.surface.unwrap(CefBrowser::class) } returns null
        val canvas = KromiumJavaFxCanvas(mockBrowser)
        assertNotNull(canvas)
        assertEquals(mockBrowser, canvas.activeBrowser)
    }

    @Test
    fun testOsrComponentLifecycle() {
        val canvas = KromiumJavaFxCanvas()
        canvas.setPopupBounds(Rectangle(10, 20, 100, 200))
        canvas.setPopupVisible(true)

        // Paint main buffer initial frame
        val buffer = ByteBuffer.allocateDirect(100 * 100 * 4)
        canvas.onPaint(buffer, 100, 100, false)

        // Paint popup buffer initial frame
        val popupBuffer = ByteBuffer.allocateDirect(50 * 50 * 4)
        canvas.onPaint(popupBuffer, 50, 50, true)

        // Paint subsequent frame without resize (triggers updateBuffer with dirty rect)
        val secondBuffer = ByteBuffer.allocateDirect(100 * 100 * 4)
        canvas.onPaint(secondBuffer, 100, 100, false)

        // Paint subsequent popup frame without resize
        val secondPopupBuffer = ByteBuffer.allocateDirect(50 * 50 * 4)
        canvas.onPaint(secondPopupBuffer, 50, 50, true)

        canvas.setPopupVisible(false)
        canvas.requestFocus()
        canvas.onCursorChange(3) // TEXT / I-beam cursor
    }

    @Test
    fun testJavaFxCanvasRegistrationAndFactory() {
        KromiumJavaFxCanvas.registerFactory()
        assertTrue(org.daviante.kromium.jcef.osr.JcefOsrPanelFactory.hasFactory(org.daviante.kromium.api.ui.KromiumUiFramework.JAVAFX))
    }
}
