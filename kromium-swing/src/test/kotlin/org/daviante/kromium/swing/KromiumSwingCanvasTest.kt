package org.daviante.kromium.swing

import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KromiumSwingCanvasTest {

    @Test
    fun testSwingCanvasInstantiationAndRegistration() {
        val canvas = KromiumSwingCanvas()
        assertTrue(JPanel::class.java.isAssignableFrom(KromiumSwingCanvas::class.java))
        assertTrue(JcefOsrComponent::class.java.isAssignableFrom(KromiumSwingCanvas::class.java))
        assertNotNull(canvas.component)

        KromiumSwingCanvas.registerFactory()
        assertTrue(JcefOsrPanelFactory.hasFactory(KromiumUiFramework.SWING))
    }
}
