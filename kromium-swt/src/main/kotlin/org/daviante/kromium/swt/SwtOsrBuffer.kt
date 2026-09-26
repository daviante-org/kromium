package org.daviante.kromium.swt

import org.daviante.kromium.jcef.osr.JcefOsrComponent
import java.awt.Rectangle
import java.nio.ByteBuffer

/**
 * Lightweight in-memory buffer used in SWT applications to capture initial OSR frames
 * before [KromiumSwtCanvas] binds to the browser instance on the SWT UI thread.
 */
class SwtOsrBuffer : JcefOsrComponent {

    @Volatile
    var lastBuffer: ByteBuffer? = null
        private set

    @Volatile
    var lastWidth: Int = 0
        private set

    @Volatile
    var lastHeight: Int = 0
        private set

    @Volatile
    var isLastPopup: Boolean = false
        private set

    @Volatile
    var popupBounds: Rectangle? = null
        private set

    @Volatile
    var isPopupVisible: Boolean = false
        private set

    override fun onPaint(buffer: ByteBuffer, width: Int, height: Int, isPopup: Boolean) {
        this.lastBuffer = buffer
        this.lastWidth = width
        this.lastHeight = height
        this.isLastPopup = isPopup
    }

    override fun setPopupBounds(rect: Rectangle) {
        this.popupBounds = rect
    }

    override fun setPopupVisible(visible: Boolean) {
        this.isPopupVisible = visible
    }
}
