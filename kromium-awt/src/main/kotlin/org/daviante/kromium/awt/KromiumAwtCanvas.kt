package org.daviante.kromium.awt

import org.cef.browser.CefBrowserOsr
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefJava2dRenderer
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrComponentFactory
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import java.awt.Canvas
import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.RenderingHints
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Dedicated, 100% pure AWT [java.awt.Canvas] for hosting Kromium Chromium OSR output.
 * Renders Chromium's pixel buffer directly via Java2D into a standard [java.awt.Canvas],
 * with zero javax.swing.* dependencies.
 */
open class KromiumAwtCanvas : Canvas(), JcefOsrComponent {

    private val renderer = JcefJava2dRenderer(this)

    override fun getComponent(): Component = this

    var bufferedImageType: Int
        get() = renderer.bufferedImageType
        set(value) { renderer.bufferedImageType = value }

    var byteOrder: ByteOrder
        get() = renderer.byteOrder
        set(value) { renderer.byteOrder = value }

    init {
        isFocusable = true
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
        cursor = awtCursor
    }

    override fun update(g: Graphics) {
        // Avoid default AWT clearing background which causes flicker
        paint(g)
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

    override fun addNotify() {
        super.addNotify()
        disablePeerBackgroundErase()
    }

    private fun disablePeerBackgroundErase() {
        try {
            val peerField = Component::class.java.getDeclaredField("peer")
            peerField.isAccessible = true
            val peer = peerField.get(this) ?: return
            val method = peer.javaClass.getMethod("disableBackgroundErase")
            method.isAccessible = true
            method.invoke(peer)
        } catch (_: Throwable) {
            // Ignored on platforms or JVMs without disableBackgroundErase
        }
    }

    override fun paint(g: Graphics) {
        if (!renderer.hasFrame()) {
            val bg = background
            if (bg != null) {
                g.color = bg
                g.fillRect(0, 0, width, height)
            }
            return
        }
        renderer.render(g, width, height)
    }

    class Factory : JcefOsrComponentFactory {
        override fun supports(framework: KromiumUiFramework): Boolean = framework == KromiumUiFramework.AWT

        override fun createComponent(browser: CefBrowserOsr, config: KromiumClientConfig?): JcefOsrComponent {
            return KromiumAwtCanvas()
        }
    }

    companion object {
        init {
            registerFactory()
        }

        @JvmStatic
        fun registerFactory() {
            JcefOsrPanelFactory.register(KromiumUiFramework.AWT, Factory())
        }
    }
}
