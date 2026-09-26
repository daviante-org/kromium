package org.daviante.kromium.jcef.osr

import org.cef.browser.CefBrowserOsr
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.error.KromiumException
import org.daviante.kromium.api.ui.KromiumUiFramework
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JcefOsrPanelFactoryTest {

    @Test
    fun testCreatePanelNullFrameworkThrowsFrameworkRequired() {
        val ex = assertFailsWith<KromiumException.FrameworkRequired> {
            JcefOsrPanelFactory.createPanel(null, null, null)
        }
        assertTrue(ex.message.contains("mandatory"))
    }

    @Test
    fun testCreatePanelHeadlessReturnsNull() {
        val result = JcefOsrPanelFactory.createPanel(KromiumUiFramework.HEADLESS, null, null)
        assertNull(result)
    }

    @Test
    fun testCreatePanelUnregisteredFrameworkThrowsCanvasNotAvailable() {
        // Ensure no factory is registered for AWT during test
        JcefOsrPanelFactory.unregister(KromiumUiFramework.AWT)

        val ex = assertFailsWith<KromiumException.CanvasNotAvailable> {
            JcefOsrPanelFactory.createPanel(KromiumUiFramework.AWT, null, null)
        }
        assertEquals(KromiumUiFramework.AWT, ex.framework)
        assertTrue(ex.message.contains("AWT"))
    }

    @Test
    fun testRegisterAndCreatePanel() {
        val dummyComponent = object : JcefOsrComponent {
            override fun onPaint(buffer: java.nio.ByteBuffer?, width: Int, height: Int, isPopup: Boolean) {}
            override fun setPopupBounds(rect: java.awt.Rectangle?) {}
            override fun setPopupVisible(visible: Boolean) {}
            override fun getUiObject(): Any = "dummyUi"
        }

        val dummyFactory = object : JcefOsrComponentFactory {
            override fun supports(framework: KromiumUiFramework): Boolean = framework == KromiumUiFramework.SWT
            override fun createComponent(browser: CefBrowserOsr?, config: KromiumClientConfig?): JcefOsrComponent {
                return dummyComponent
            }
        }

        try {
            JcefOsrPanelFactory.register(KromiumUiFramework.SWT, dummyFactory)
            assertTrue(JcefOsrPanelFactory.hasFactory(KromiumUiFramework.SWT))

            val comp = JcefOsrPanelFactory.createPanel(KromiumUiFramework.SWT, null, null)
            assertSame(dummyComponent, comp)
        } finally {
            JcefOsrPanelFactory.unregister(KromiumUiFramework.SWT)
        }
    }
}
