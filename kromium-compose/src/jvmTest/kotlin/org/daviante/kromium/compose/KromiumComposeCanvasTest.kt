package org.daviante.kromium.compose

import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KromiumComposeCanvasTest {

    @Test
    fun testComposeCanvasInstantiationAndRegistration() {
        val canvas = KromiumComposeCanvas()
        assertTrue(JPanel::class.java.isAssignableFrom(KromiumComposeCanvas::class.java))
        assertTrue(JcefOsrComponent::class.java.isAssignableFrom(KromiumComposeCanvas::class.java))
        assertNotNull(canvas.component)

        KromiumComposeCanvas.registerFactory()
        assertTrue(JcefOsrPanelFactory.hasFactory(KromiumUiFramework.COMPOSE))
    }
}
