package org.daviante.kromium.javafx

import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrComponentFactory
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import java.awt.Component
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.InputEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.MouseWheelEvent as AwtMouseWheelEvent
import java.nio.ByteBuffer

/**
 * A 100% native JavaFX [StackPane] component for hosting Chromium OSR output.
 *
 * Implements [JcefOsrComponent] to participate directly in Chromium's OSR rendering pipeline,
 * including full support for HTML `<select>` dropdowns and context popups.
 *
 * Renders raw Chromium BGRA pixel buffers directly via JavaFX [PixelBuffer] and [WritableImage],
 * leveraging Prism GPU hardware acceleration with zero Swing/AWT embedding, zero [javafx.embed.swing.SwingNode],
 * and zero foreign window handle conflicts.
 */
class KromiumJavaFxCanvas @JvmOverloads constructor(
    browser: KromiumBrowser? = null
) : StackPane(), JcefOsrComponent {

    private val fallbackComponent: Component = object : Component() {}

    private val imageView = ImageView().apply {
        isPreserveRatio = false
        isSmooth = false
    }

    private val popupImageView = ImageView().apply {
        isPreserveRatio = false
        isSmooth = false
        isVisible = false
        isManaged = false
    }

    private val bufferLock = Any()
    private var stagingBuffer: ByteBuffer? = null
    private var directBuffer: ByteBuffer? = null
    private var pixelBuffer: PixelBuffer<ByteBuffer>? = null
    private var writableImage: WritableImage? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var bufferReallocated = false
    private var hasNewFrame = false
    private var renderScheduled = false

    private var popupStagingBuffer: ByteBuffer? = null
    private var popupDirectBuffer: ByteBuffer? = null
    private var popupPixelBuffer: PixelBuffer<ByteBuffer>? = null
    private var popupWritableImage: WritableImage? = null
    private var popupFrameWidth = 0
    private var popupFrameHeight = 0
    private var popupReallocated = false
    private var hasNewPopupFrame = false
    private var popupRenderScheduled = false

    var activeBrowser: KromiumBrowser? = null
        private set

    private var cefBrowser: CefBrowser? = null
    private var cefBrowserOsr: CefBrowserOsr? = null

    init {
        isFocusTraversable = true
        children.addAll(imageView, popupImageView)

        // Bind main image dimensions to fill this container
        imageView.fitWidthProperty().bind(widthProperty())
        imageView.fitHeightProperty().bind(heightProperty())

        setupListeners()

        if (browser != null) {
            attach(browser)
        }
    }

    /**
     * Attaches this native JavaFX canvas to the given [KromiumBrowser].
     */
    fun attach(browser: KromiumBrowser) {
        this.activeBrowser = browser

        val unwrappedCef = browser.view.surface.unwrap(CefBrowser::class)
        this.cefBrowser = unwrappedCef
        val osr = unwrappedCef as? CefBrowserOsr
        this.cefBrowserOsr = osr
        osr?.setOsrComponent(this, this)

        // Install 1:1 OSR keyboard bridge connecting JavaFX events to Kromium
        KromiumJavaFxKeyboardBridge.install(this, browser)

        // Configure initial scale factor
        val scale = scene?.window?.outputScaleX ?: Screen.getPrimary()?.outputScaleX ?: 1.0
        osr?.setScaleFactor(scale)

        val w = width.toInt()
        val h = height.toInt()
        if (w > 0 && h > 0) {
            osr?.notifyResized(w, h)
        }
        osr?.createImmediately()
        if (isFocused) {
            cefBrowser?.setFocus(true)
        }
    }

    /**
     * Directly binds this canvas to the underlying [CefBrowserOsr] instance.
     */
    fun attachCefOsr(osr: CefBrowserOsr) {
        this.cefBrowser = osr
        this.cefBrowserOsr = osr
        osr.setOsrComponent(this, this)

        val scale = scene?.window?.outputScaleX ?: Screen.getPrimary()?.outputScaleX ?: 1.0
        osr.setScaleFactor(scale)

        val w = width.toInt()
        val h = height.toInt()
        if (w > 0 && h > 0) {
            osr.notifyResized(w, h)
        }
        osr.createImmediately()
        if (isFocused) {
            cefBrowser?.setFocus(true)
        }
    }

    // --- JcefOsrComponent Implementation ---

    override fun getUiObject(): Any = this

    override fun requestFocus() {
        if (Platform.isFxApplicationThread()) {
            super<StackPane>.requestFocus()
        } else {
            Platform.runLater {
                super<StackPane>.requestFocus()
            }
        }
    }

    override fun onPaint(buffer: ByteBuffer, width: Int, height: Int, isPopup: Boolean) {
        if (width <= 0 || height <= 0) return
        val byteCount = width * height * 4

        if (isPopup) {
            synchronized(bufferLock) {
                if (popupStagingBuffer == null || popupStagingBuffer!!.capacity() < byteCount) {
                    popupStagingBuffer = ByteBuffer.allocateDirect(byteCount)
                }
                if (popupFrameWidth != width || popupFrameHeight != height) {
                    popupFrameWidth = width
                    popupFrameHeight = height
                    popupReallocated = true
                }
                popupStagingBuffer!!.clear()
                val oldLimit = buffer.limit()
                val toCopy = Math.min(buffer.remaining(), byteCount)
                buffer.limit(buffer.position() + toCopy)
                popupStagingBuffer!!.put(buffer)
                buffer.limit(oldLimit)
                popupStagingBuffer!!.position(0)
                popupStagingBuffer!!.limit(toCopy)
                hasNewPopupFrame = true

                if (!popupRenderScheduled) {
                    popupRenderScheduled = true
                    Platform.runLater { processPopupFrame() }
                }
            }
        } else {
            synchronized(bufferLock) {
                if (stagingBuffer == null || stagingBuffer!!.capacity() < byteCount) {
                    stagingBuffer = ByteBuffer.allocateDirect(byteCount)
                }
                if (frameWidth != width || frameHeight != height) {
                    frameWidth = width
                    frameHeight = height
                    bufferReallocated = true
                }
                stagingBuffer!!.clear()
                val oldLimit = buffer.limit()
                val toCopy = Math.min(buffer.remaining(), byteCount)
                buffer.limit(buffer.position() + toCopy)
                stagingBuffer!!.put(buffer)
                buffer.limit(oldLimit)
                stagingBuffer!!.position(0)
                stagingBuffer!!.limit(toCopy)
                hasNewFrame = true

                if (!renderScheduled) {
                    renderScheduled = true
                    Platform.runLater { processMainFrame() }
                }
            }
        }
    }

    private fun processMainFrame() {
        synchronized(bufferLock) {
            renderScheduled = false
            if (!hasNewFrame) return
            hasNewFrame = false

            val staging = stagingBuffer ?: return
            val w = frameWidth
            val h = frameHeight
            if (w <= 0 || h <= 0) return
            val byteCount = w * h * 4

            if (directBuffer == null || directBuffer!!.capacity() < byteCount) {
                directBuffer = ByteBuffer.allocateDirect(byteCount)
                bufferReallocated = true
            }

            val direct = directBuffer ?: return

            if (bufferReallocated || pixelBuffer == null) {
                bufferReallocated = false
                direct.clear()
                staging.position(0)
                val count = Math.min(staging.remaining(), byteCount)
                staging.limit(staging.position() + count)
                direct.put(staging)
                direct.position(0)
                direct.limit(byteCount)

                val pb = PixelBuffer(w, h, direct, PixelFormat.getByteBgraPreInstance())
                pixelBuffer = pb
                val img = WritableImage(pb)
                writableImage = img
                imageView.image = img
            } else {
                pixelBuffer?.updateBuffer {
                    direct.clear()
                    staging.position(0)
                    val count = Math.min(staging.remaining(), byteCount)
                    staging.limit(staging.position() + count)
                    direct.put(staging)
                    direct.position(0)
                    direct.limit(byteCount)
                    Rectangle2D(0.0, 0.0, w.toDouble(), h.toDouble())
                }
            }
        }
    }

    private fun processPopupFrame() {
        synchronized(bufferLock) {
            popupRenderScheduled = false
            if (!hasNewPopupFrame) return
            hasNewPopupFrame = false

            val staging = popupStagingBuffer ?: return
            val w = popupFrameWidth
            val h = popupFrameHeight
            if (w <= 0 || h <= 0) return
            val byteCount = w * h * 4

            if (popupDirectBuffer == null || popupDirectBuffer!!.capacity() < byteCount) {
                popupDirectBuffer = ByteBuffer.allocateDirect(byteCount)
                popupReallocated = true
            }

            val direct = popupDirectBuffer ?: return

            if (popupReallocated || popupPixelBuffer == null) {
                popupReallocated = false
                direct.clear()
                staging.position(0)
                val count = Math.min(staging.remaining(), byteCount)
                staging.limit(staging.position() + count)
                direct.put(staging)
                direct.position(0)
                direct.limit(byteCount)

                val pb = PixelBuffer(w, h, direct, PixelFormat.getByteBgraPreInstance())
                popupPixelBuffer = pb
                val img = WritableImage(pb)
                popupWritableImage = img
                popupImageView.image = img
            } else {
                popupPixelBuffer?.updateBuffer {
                    direct.clear()
                    staging.position(0)
                    val count = Math.min(staging.remaining(), byteCount)
                    staging.limit(staging.position() + count)
                    direct.put(staging)
                    direct.position(0)
                    direct.limit(byteCount)
                    Rectangle2D(0.0, 0.0, w.toDouble(), h.toDouble())
                }
            }
        }
    }

    override fun setPopupBounds(rect: Rectangle) {
        Platform.runLater {
            popupImageView.translateX = rect.x.toDouble()
            popupImageView.translateY = rect.y.toDouble()
            popupImageView.fitWidth = rect.width.toDouble()
            popupImageView.fitHeight = rect.height.toDouble()
        }
    }

    override fun setPopupVisible(visible: Boolean) {
        Platform.runLater {
            popupImageView.isVisible = visible
        }
    }

    override fun setInterpolation(interpolationHint: Any?) {
        val smooth = interpolationHint == RenderingHints.VALUE_INTERPOLATION_BICUBIC 
                || interpolationHint == RenderingHints.VALUE_INTERPOLATION_BILINEAR
        imageView.isSmooth = smooth
        popupImageView.isSmooth = smooth
    }

    override fun onCursorChange(cursorType: Int) {
        val fxCursor = when (cursorType) {
            0 -> javafx.scene.Cursor.DEFAULT
            1 -> javafx.scene.Cursor.CROSSHAIR
            2 -> javafx.scene.Cursor.HAND
            3 -> javafx.scene.Cursor.TEXT // I-beam pointer for text inputs
            4 -> javafx.scene.Cursor.WAIT
            6, 13, 15 -> javafx.scene.Cursor.H_RESIZE
            7, 10, 14 -> javafx.scene.Cursor.V_RESIZE
            8, 12 -> javafx.scene.Cursor.NE_RESIZE
            9, 11 -> javafx.scene.Cursor.NW_RESIZE
            29 -> javafx.scene.Cursor.MOVE
            else -> javafx.scene.Cursor.DEFAULT
        }
        Platform.runLater {
            cursor = fxCursor
        }
    }

    private fun setupListeners() {
        // Synchronize JavaFX focus with CEF
        focusedProperty().addListener { _, _, isFocused ->
            cefBrowser?.setFocus(isFocused)
        }

        // Resize listeners: propagate dimensions to CEF OSR engine
        widthProperty().addListener { _, _, newW ->
            notifyResize(newW.toInt(), height.toInt())
        }
        heightProperty().addListener { _, _, newH ->
            notifyResize(width.toInt(), newH.toInt())
        }

        // DPI scale updates when moving across screens
        sceneProperty().addListener { _, _, newScene ->
            if (newScene != null) {
                newScene.windowProperty().addListener { _, _, newWin ->
                    if (newWin != null) {
                        newWin.outputScaleXProperty().addListener { _, _, newScale ->
                            cefBrowserOsr?.setScaleFactor(newScale.toDouble())
                        }
                        cefBrowserOsr?.setScaleFactor(newWin.outputScaleX)
                    }
                }
            }
        }

        // Mouse listeners: forward native JavaFX mouse events directly to CEF
        setOnMousePressed { e ->
            requestFocus()
            cefBrowser?.setFocus(true)
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_PRESSED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseReleased { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_RELEASED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseMoved { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_MOVED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseDragged { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_DRAGGED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseClicked { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_CLICKED, clickCount = e.clickCount)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseEntered { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_ENTERED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        setOnMouseExited { e ->
            val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_EXITED)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        // Scroll listener: forward wheel rotation to CEF
        setOnScroll { e ->
            val browser = cefBrowser ?: return@setOnScroll
            val modifiers = getAwtModifiers(e)
            // In JavaFX, deltaY is negative when scrolling down (e.g. -32.0 to -40.0 on standard wheel tick).
            // Chromium OSR expects deltaPixels in wheelRotation with scrollAmount = 1.
            // Scale by 2.5 so a standard wheel tick produces ~100px of scrolling.
            val scrollPixels = if (e.deltaY != 0.0) {
                -(e.deltaY * 2.5).toInt().let { if (it == 0) (if (e.deltaY > 0) -100 else 100) else it }
            } else 0

            val wheelEvent = AwtMouseWheelEvent(
                fallbackComponent,
                AwtMouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                modifiers,
                e.x.toInt(),
                e.y.toInt(),
                0,
                false,
                AwtMouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                scrollPixels
            )
            browser.sendMouseWheelEvent(wheelEvent)
        }
    }

    private fun notifyResize(w: Int, h: Int) {
        if (w > 0 && h > 0) {
            cefBrowserOsr?.notifyResized(w, h) ?: cefBrowser?.wasResized(w, h)
        }
    }

    private fun toAwtMouseEvent(
        e: javafx.scene.input.MouseEvent,
        id: Int,
        clickCount: Int = e.clickCount.coerceAtLeast(1)
    ): AwtMouseEvent {
        val button = when (e.button) {
            MouseButton.PRIMARY -> AwtMouseEvent.BUTTON1
            MouseButton.MIDDLE -> AwtMouseEvent.BUTTON2
            MouseButton.SECONDARY -> AwtMouseEvent.BUTTON3
            else -> AwtMouseEvent.NOBUTTON
        }
        val modifiers = getAwtModifiers(e, id)
        return AwtMouseEvent(
            fallbackComponent,
            id,
            System.currentTimeMillis(),
            modifiers,
            e.x.toInt(),
            e.y.toInt(),
            clickCount,
            button == AwtMouseEvent.BUTTON3,
            button
        )
    }

    private fun getAwtModifiers(e: javafx.scene.input.MouseEvent, id: Int): Int {
        var modifiers = 0
        when (id) {
            AwtMouseEvent.MOUSE_PRESSED -> {
                if (e.button == MouseButton.PRIMARY || e.isPrimaryButtonDown) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
                if (e.button == MouseButton.MIDDLE || e.isMiddleButtonDown) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
                if (e.button == MouseButton.SECONDARY || e.isSecondaryButtonDown) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
            }
            AwtMouseEvent.MOUSE_RELEASED -> {
                // When released, the button that was released is no longer down
                if (e.isPrimaryButtonDown && e.button != MouseButton.PRIMARY) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
                if (e.isMiddleButtonDown && e.button != MouseButton.MIDDLE) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
                if (e.isSecondaryButtonDown && e.button != MouseButton.SECONDARY) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
            }
            else -> {
                if (e.isPrimaryButtonDown) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
                if (e.isMiddleButtonDown) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
                if (e.isSecondaryButtonDown) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
            }
        }
        if (e.isShiftDown) modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        if (e.isControlDown) modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        if (e.isAltDown) modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        if (e.isMetaDown) modifiers = modifiers or InputEvent.META_DOWN_MASK
        return modifiers
    }

    private fun getAwtModifiers(e: javafx.scene.input.ScrollEvent): Int {
        var modifiers = 0
        if (e.isShiftDown) modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        if (e.isControlDown) modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        if (e.isAltDown) modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        if (e.isMetaDown) modifiers = modifiers or InputEvent.META_DOWN_MASK
        return modifiers
    }

    class Factory : JcefOsrComponentFactory {
        override fun supports(framework: KromiumUiFramework): Boolean = framework == KromiumUiFramework.JAVAFX

        override fun createComponent(browser: CefBrowserOsr, config: KromiumClientConfig?): JcefOsrComponent {
            val canvas = KromiumJavaFxCanvas()
            canvas.attachCefOsr(browser)
            return canvas
        }
    }

    companion object {
        init {
            registerFactory()
        }

        @JvmStatic
        fun registerFactory() {
            JcefOsrPanelFactory.register(KromiumUiFramework.JAVAFX, Factory())
        }
    }
}
