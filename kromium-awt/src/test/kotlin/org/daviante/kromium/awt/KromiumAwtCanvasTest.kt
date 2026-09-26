package org.daviante.kromium.awt

import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import java.awt.Canvas
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KromiumAwtCanvasTest {

    @Test
    fun testAwtCanvasInstantiationAndRegistration() {
        val canvas = KromiumAwtCanvas()
        assertTrue(Canvas::class.java.isAssignableFrom(KromiumAwtCanvas::class.java))
        assertTrue(JcefOsrComponent::class.java.isAssignableFrom(KromiumAwtCanvas::class.java))
        assertNotNull(canvas.component)

        KromiumAwtCanvas.registerFactory()
        assertTrue(JcefOsrPanelFactory.hasFactory(KromiumUiFramework.AWT))
    }
}
