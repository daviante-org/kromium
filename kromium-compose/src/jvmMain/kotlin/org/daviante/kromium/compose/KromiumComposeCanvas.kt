package org.daviante.kromium.compose

import org.cef.browser.CefBrowserOsr
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefJava2dRenderer
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrComponentFactory
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.RenderingHints
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.swing.JPanel

/**
 * Dedicated Jetpack Compose Desktop canvas for hosting Kromium Chromium OSR output.
 * Fully independent of kromium-swing; renders via JcefJava2dRenderer and participates
 * directly in Compose desktop [KromiumView] hierarchies.
 */
open class KromiumComposeCanvas : JPanel(), JcefOsrComponent {

    private val renderer = JcefJava2dRenderer(this)

    override fun getComponent(): Component = this

    var bufferedImageType: Int
        get() = renderer.bufferedImageType
        set(value) { renderer.bufferedImageType = value }

    var byteOrder: ByteOrder
        get() = renderer.byteOrder
        set(value) { renderer.byteOrder = value }

    init {
        isOpaque = true
        isDoubleBuffered = true
    }

    override fun setRenderingHint(key: RenderingHints.Key, value: Any?) {
        renderer.setRenderingHint(key, value)
    }

    fun getRenderingHint(key: RenderingHints.Key): Any? = renderer.getRenderingHint(key)

    override fun setInterpolation(interpolationHint: Any) {
        renderer.setInterpolation(interpolationHint)
    }

    override fun requestFocus() {
        requestFocusInWindow()
    }

    override fun onCursorChange(cursorType: Int) {
        val awtCursor = CefBrowserOsr.mapCefCursorToAwt(cursorType)
        javax.swing.SwingUtilities.invokeLater {
            cursor = awtCursor
        }
    }

    override fun onPaint(buffer: ByteBuffer, width: Int, height: Int, isPopup: Boolean) {
        renderer.onPaint(buffer, width, height, isPopup)
    }

    override fun setPopupBounds(rect: Rectangle) {
        renderer.setPopupBounds(rect)
    }

    override fun setPopupVisible(visible: Boolean) {
        renderer.setPopupVisible(visible)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        renderer.render(g, width, height)
    }

    class Factory : JcefOsrComponentFactory {
        override fun supports(framework: KromiumUiFramework): Boolean = framework == KromiumUiFramework.COMPOSE

        override fun createComponent(browser: CefBrowserOsr, config: KromiumClientConfig?): JcefOsrComponent {
            val canvas = KromiumComposeCanvas()
            if (config?.isTransparent == true) {
                canvas.isOpaque = false
            }
            return canvas
        }
    }

    companion object {
        init {
            registerFactory()
        }

        @JvmStatic
        fun registerFactory() {
            JcefOsrPanelFactory.register(KromiumUiFramework.COMPOSE, Factory())
        }
    }
}
