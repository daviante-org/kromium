package org.daviante.kromium.swt

import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserOsr
import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import org.daviante.kromium.jcef.osr.JcefOsrComponent
import org.daviante.kromium.jcef.osr.JcefOsrComponentFactory
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory
import org.eclipse.swt.SWT
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.ImageDataProvider
import org.eclipse.swt.graphics.PaletteData
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Composite
import java.awt.Component
import java.awt.Rectangle
import java.awt.event.InputEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.MouseWheelEvent as AwtMouseWheelEvent
import java.lang.reflect.Method
import java.nio.ByteBuffer

/**
 * A 100% native Eclipse SWT [Canvas] for hosting Kromium Chromium OSR output.
 *
 * Implements [JcefOsrComponent] to participate directly in Chromium's OSR rendering pipeline.
 *
 * Renders raw Chromium BGRA pixel buffers directly via SWT [Image] and [org.eclipse.swt.graphics.GC],
 * avoiding any foreign window embedding (such as SWT_AWT), Cocoa first-responder deadlocks,
 * or NSWindow warnings on macOS.
 */
class KromiumSwtCanvas @JvmOverloads constructor(
    parent: Composite,
    style: Int = SWT.NO_BACKGROUND or SWT.DOUBLE_BUFFERED,
    browser: KromiumBrowser? = null
) : Canvas(parent, style), JcefOsrComponent {

    private val fallbackComponent: Component = object : Component() {}
    private val palette = PaletteData(0x0000FF00, 0x00FF0000, 0xFF000000.toInt())

    private val bufferLock = Any()
    private var frontData: ByteArray? = null
    private var frontWidth: Int = 0
    private var frontHeight: Int = 0
    private var hasNewFrame: Boolean = false
    private var currentImage: Image? = null

    private var popupVisible: Boolean = false
    private var popupRect: Rectangle? = null

    @Volatile
    var activeBrowser: KromiumBrowser? = null
        private set

    private var cefBrowser: CefBrowser? = null
    private var cefBrowserOsr: CefBrowserOsr? = null

    @Volatile
    private var lastCursorType: Int = -1

    private var lastWheelTime: Long = 0
    private var wheelRemainderY: Double = 0.0
    private var wheelRemainderX: Double = 0.0

    init {
        setupListeners()
        if (browser != null) {
            attach(browser)
        }
    }

    /**
     * Accurately detects the device HiDPI scale factor across platforms.
     *
     * Uses SWT's HiDPI DPIUtil / Monitor zoom on Retina displays (yielding 2.0 on macOS Retina,
     * instead of 0.75 which would be caused by 72dpi/96dpi).
     */
    fun detectDeviceScaleFactor(): Double {
        // 1. Check SWT's DPIUtil.getDeviceZoom() (returns 100, 150, 200, etc.)
        try {
            val dpiUtilClass = Class.forName("org.eclipse.swt.internal.DPIUtil")
            val getDeviceZoomMethod = dpiUtilClass.getMethod("getDeviceZoom")
            val zoom = (getDeviceZoomMethod.invoke(null) as Number).toInt()
            if (zoom > 0) {
                return zoom / 100.0
            }
        } catch (_: Throwable) {}

        // 2. Check primary monitor zoom if available (SWT 4.25+)
        try {
            val monitor = display.primaryMonitor
            val getZoomMethod = monitor.javaClass.getMethod("getZoom")
            val zoom = (getZoomMethod.invoke(monitor) as Number).toInt()
            if (zoom > 0) {
                return zoom / 100.0
            }
        } catch (_: Throwable) {}

        // 3. Check macOS Cocoa backingScaleFactor via reflection
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            try {
                val screenClass = Class.forName("org.eclipse.swt.internal.cocoa.NSScreen")
                val mainScreenMethod = screenClass.getMethod("mainScreen")
                val screen = mainScreenMethod.invoke(null)
                if (screen != null) {
                    val scaleMethod = screen.javaClass.getMethod("backingScaleFactor")
                    val scale = (scaleMethod.invoke(screen) as Number).toDouble()
                    if (scale > 0.0) return scale
                }
            } catch (_: Throwable) {}
            return 2.0 // Safe fallback for macOS Retina displays
        }

        // 4. Fallback to display DPI (96 DPI baseline for Windows / Linux)
        val dpi = display.dpi.x
        val scale = dpi / 96.0
        return if (scale <= 0.0) 1.0 else scale
    }

    /**
     * Attaches this native SWT canvas to the given [KromiumBrowser].
     */
    fun attach(browser: KromiumBrowser) {
        this.activeBrowser = browser

        val unwrappedCef = browser.view.surface.unwrap(CefBrowser::class)
        this.cefBrowser = unwrappedCef
        val osr = unwrappedCef as? CefBrowserOsr
        this.cefBrowserOsr = osr
        val oldComponent = osr?.osrComponent
        osr?.setOsrComponent(this, this)
        if (oldComponent is SwtOsrBuffer) {
            val buf = oldComponent.lastBuffer
            if (buf != null && oldComponent.lastWidth > 0 && oldComponent.lastHeight > 0) {
                onPaint(buf, oldComponent.lastWidth, oldComponent.lastHeight, oldComponent.isLastPopup)
            }
        }

        // Configure initial DPI and size
        if (!isDisposed) {
            val scale = detectDeviceScaleFactor()
            osr?.setScaleFactor(scale)

            val ca = clientArea
            if (ca.width > 0 && ca.height > 0) {
                osr?.notifyResized(ca.width, ca.height)
            }
            osr?.createImmediately()
        }
    }

    /**
     * Directly binds this canvas to the underlying [CefBrowserOsr] instance.
     */
    fun attachCefOsr(osr: CefBrowserOsr) {
        this.cefBrowser = osr
        this.cefBrowserOsr = osr
        val oldComponent = osr.osrComponent
        osr.setOsrComponent(this, this)
        if (oldComponent is SwtOsrBuffer) {
            val buf = oldComponent.lastBuffer
            if (buf != null && oldComponent.lastWidth > 0 && oldComponent.lastHeight > 0) {
                onPaint(buf, oldComponent.lastWidth, oldComponent.lastHeight, oldComponent.isLastPopup)
            }
        }

        if (!isDisposed) {
            val scale = detectDeviceScaleFactor()
            osr.setScaleFactor(scale)

            val ca = clientArea
            if (ca.width > 0 && ca.height > 0) {
                osr.notifyResized(ca.width, ca.height)
            }
            osr.createImmediately()
        }
    }

    // --- JcefOsrComponent Implementation ---

    override fun getUiObject(): Any = this

    override fun onPaint(buffer: ByteBuffer, width: Int, height: Int, isPopup: Boolean) {
        if (width <= 0 || height <= 0) return
        val byteCount = width * height * 4

        synchronized(bufferLock) {
            if (frontData == null || frontData!!.size != byteCount) {
                frontData = ByteArray(byteCount)
            }
            buffer.position(0)
            val toRead = Math.min(buffer.remaining(), byteCount)
            buffer.get(frontData, 0, toRead)
            frontWidth = width
            frontHeight = height
            hasNewFrame = true
        }

        if (!isDisposed) {
            display.asyncExec {
                if (!isDisposed) {
                    redraw()
                }
            }
        }
    }

    override fun setPopupBounds(rect: Rectangle) {
        this.popupRect = rect
    }

    override fun setPopupVisible(visible: Boolean) {
        this.popupVisible = visible
        if (!isDisposed) {
            display.asyncExec {
                if (!isDisposed) {
                    redraw()
                }
            }
        }
    }

    override fun requestFocus() {
        if (!isDisposed) {
            display.asyncExec {
                if (!isDisposed) {
                    forceFocus()
                }
            }
        }
    }

    override fun onCursorChange(cursorType: Int) {
        if (isDisposed) return
        if (lastCursorType == cursorType) return
        lastCursorType = cursorType

        display.asyncExec {
            if (!isDisposed) {
                val swtCursorType = toSwtCursor(cursorType)
                val newCursor = display.getSystemCursor(swtCursorType)
                if (cursor != newCursor) {
                    cursor = newCursor
                }
                display.update()
            }
        }
    }

    private fun setupListeners() {
        // Paint listener: draw the latest frame from Chromium with HiDPI support
        addPaintListener { e ->
            var imageToDraw: Image? = null
            var srcW = 0
            var srcH = 0

            synchronized(bufferLock) {
                if (hasNewFrame && frontData != null && frontWidth > 0 && frontHeight > 0) {
                    try {
                        val dataCopy = frontData!!.clone()
                        val currentW = frontWidth
                        val currentH = frontHeight
                        val provider = ImageDataProvider { zoom ->
                            ImageData(
                                currentW,
                                currentH,
                                32,
                                palette,
                                4,
                                dataCopy
                            )
                        }
                        val newImage = Image(display, provider)
                        currentImage?.dispose()
                        currentImage = newImage
                        hasNewFrame = false
                    } catch (_: Throwable) {
                        try {
                            val imageData = ImageData(
                                frontWidth,
                                frontHeight,
                                32,
                                palette,
                                4,
                                frontData!!.clone()
                            )
                            val newImage = Image(display, imageData)
                            currentImage?.dispose()
                            currentImage = newImage
                            hasNewFrame = false
                        } catch (_: Throwable) {
                            // Ignore transient rendering decode errors
                        }
                    }
                }
                imageToDraw = currentImage
                srcW = frontWidth
                srcH = frontHeight
            }

            if (imageToDraw != null && !imageToDraw!!.isDisposed && srcW > 0 && srcH > 0) {
                val ca = clientArea
                e.gc.interpolation = SWT.NONE
                val bounds = imageToDraw!!.bounds
                e.gc.drawImage(imageToDraw, 0, 0, bounds.width, bounds.height, 0, 0, ca.width, ca.height)
            }
        }

        // Resize listener: notify CEF of view bounds and DPI scale changes
        addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                val ca = clientArea
                if (ca.width > 0 && ca.height > 0) {
                    val scale = detectDeviceScaleFactor()
                    cefBrowserOsr?.setScaleFactor(scale)
                    cefBrowserOsr?.notifyResized(ca.width, ca.height)
                        ?: cefBrowser?.wasResized(ca.width, ca.height)
                }
            }
        })

        // Focus listener: notify CEF focus state
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                cefBrowser?.setFocus(true)
            }

            override fun focusLost(e: FocusEvent) {
                cefBrowser?.setFocus(false)
            }
        })

        // Mouse listeners: forward native SWT mouse events directly to CEF
        addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: org.eclipse.swt.events.MouseEvent) {
                forceFocus()
                cefBrowser?.setFocus(true)
                val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_PRESSED)
                cefBrowser?.sendMouseEvent(awtEvent)
            }

            override fun mouseUp(e: org.eclipse.swt.events.MouseEvent) {
                val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_RELEASED)
                cefBrowser?.sendMouseEvent(awtEvent)
            }

            override fun mouseDoubleClick(e: org.eclipse.swt.events.MouseEvent) {
                val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_CLICKED, clickCount = 2)
                cefBrowser?.sendMouseEvent(awtEvent)
            }
        })

        addMouseMoveListener { e ->
            val isButtonDown = (e.stateMask and (SWT.BUTTON1 or SWT.BUTTON2 or SWT.BUTTON3)) != 0
            val id = if (isButtonDown) AwtMouseEvent.MOUSE_DRAGGED else AwtMouseEvent.MOUSE_MOVED
            val awtEvent = toAwtMouseEvent(e, id)
            cefBrowser?.sendMouseEvent(awtEvent)
        }

        addMouseTrackListener(object : MouseTrackAdapter() {
            override fun mouseEnter(e: org.eclipse.swt.events.MouseEvent) {
                val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_ENTERED)
                cefBrowser?.sendMouseEvent(awtEvent)
            }

            override fun mouseExit(e: org.eclipse.swt.events.MouseEvent) {
                val awtEvent = toAwtMouseEvent(e, AwtMouseEvent.MOUSE_EXITED)
                cefBrowser?.sendMouseEvent(awtEvent)
                if (!isDisposed) {
                    lastCursorType = -1
                    cursor = null
                }
            }
        })

        // Vertical scroll listener
        addMouseWheelListener { e ->
            dispatchScrollEvent(e.x, e.y, e.count, e.stateMask, isHorizontal = false)
        }

        // Horizontal scroll listener (trackpad two-finger horizontal gestures and horizontal wheel)
        addListener(SWT.MouseHorizontalWheel) { event ->
            dispatchScrollEvent(event.x, event.y, event.count, event.stateMask, isHorizontal = true)
        }

        // Keyboard listener: forward unified Kromium key events to browser
        addKeyListener(object : KeyListener {
            override fun keyPressed(e: org.eclipse.swt.events.KeyEvent) {
                val b = activeBrowser ?: return
                KromiumSwtKeyboardBridge.dispatchSwtKeyEvent(b, e, isKeyDown = true)
            }

            override fun keyReleased(e: org.eclipse.swt.events.KeyEvent) {
                val b = activeBrowser ?: return
                KromiumSwtKeyboardBridge.dispatchSwtKeyEvent(b, e, isKeyDown = false)
            }
        })

        // Dispose listener: clean up native image handle
        addDisposeListener {
            currentImage?.dispose()
            currentImage = null
        }
    }

    /**
     * Accurately converts SWT mouse wheel and horizontal wheel gestures into standard CEF scroll events.
     *
     * Corrects macOS continuous trackpad acceleration runaway, preserves natural scroll sign mapping,
     * and accumulates fractional sub-pixel scroll deltas.
     */
    private fun dispatchScrollEvent(
        x: Int,
        y: Int,
        count: Int,
        stateMask: Int,
        isHorizontal: Boolean
    ) {
        val browser = cefBrowser ?: return
        if (count == 0) return

        val now = System.currentTimeMillis()
        val dt = now - lastWheelTime
        lastWheelTime = now

        // 1. Check native macOS Cocoa deltas if available
        val nativeDelta = MacScrollHelper.getScrollDelta(isHorizontal)

        val pixelDelta: Double
        if (nativeDelta != null && nativeDelta.isPrecise) {
            // Continuous trackpad gesture on macOS: scale gentle points
            pixelDelta = nativeDelta.delta * 1.5
        } else if (dt in 1..40) {
            // High-frequency continuous stream (trackpad gesture on platforms without native delta API)
            pixelDelta = if (count > 0) 18.0 else -18.0
        } else {
            // Discrete wheel notch (physical mouse wheel)
            val notchMultiplier = if (Math.abs(count) == 1) 100.0 else 35.0
            pixelDelta = count * notchMultiplier
        }

        val totalDelta = if (isHorizontal) {
            val total = pixelDelta + wheelRemainderX
            val pixels = total.toInt()
            wheelRemainderX = total - pixels
            pixels
        } else {
            val total = pixelDelta + wheelRemainderY
            val pixels = total.toInt()
            wheelRemainderY = total - pixels
            pixels
        }

        if (totalDelta == 0) return

        var modifiers = getAwtModifiers(stateMask)
        if (isHorizontal) {
            modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        }

        // Chromium expects: positive deltaY = scroll up, negative deltaY = scroll down.
        // CEF's native C++ SendMouseWheelEvent calculates: delta = -1 * scrollAmount * wheelRotation.
        // Therefore, passing wheelRotation = -totalDelta causes Chromium to receive +totalDelta,
        // which perfectly preserves the scroll direction!
        val wheelEvent = AwtMouseWheelEvent(
            fallbackComponent,
            AwtMouseEvent.MOUSE_WHEEL,
            now,
            modifiers,
            x,
            y,
            0,
            false,
            AwtMouseWheelEvent.WHEEL_UNIT_SCROLL,
            1,
            -totalDelta
        )
        browser.sendMouseWheelEvent(wheelEvent)
    }

    private fun toAwtMouseEvent(
        e: org.eclipse.swt.events.MouseEvent,
        id: Int,
        clickCount: Int = 1
    ): AwtMouseEvent {
        val button = when (e.button) {
            1 -> AwtMouseEvent.BUTTON1
            2 -> AwtMouseEvent.BUTTON2
            3 -> AwtMouseEvent.BUTTON3
            else -> AwtMouseEvent.NOBUTTON
        }
        val modifiers = getAwtModifiers(e.stateMask, e.button, id)
        return AwtMouseEvent(
            fallbackComponent,
            id,
            System.currentTimeMillis(),
            modifiers,
            e.x,
            e.y,
            clickCount,
            button == AwtMouseEvent.BUTTON3,
            button
        )
    }

    private fun getAwtModifiers(stateMask: Int, button: Int = 0, id: Int = 0): Int {
        var modifiers = 0
        if (id == AwtMouseEvent.MOUSE_PRESSED) {
            if (button == 1 || (stateMask and SWT.BUTTON1 != 0)) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
            if (button == 2 || (stateMask and SWT.BUTTON2 != 0)) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
            if (button == 3 || (stateMask and SWT.BUTTON3 != 0)) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
        } else if (id == AwtMouseEvent.MOUSE_RELEASED) {
            if (button != 1 && (stateMask and SWT.BUTTON1 != 0)) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
            if (button != 2 && (stateMask and SWT.BUTTON2 != 0)) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
            if (button != 3 && (stateMask and SWT.BUTTON3 != 0)) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
        } else {
            if (stateMask and SWT.BUTTON1 != 0) modifiers = modifiers or InputEvent.BUTTON1_DOWN_MASK
            if (stateMask and SWT.BUTTON2 != 0) modifiers = modifiers or InputEvent.BUTTON2_DOWN_MASK
            if (stateMask and SWT.BUTTON3 != 0) modifiers = modifiers or InputEvent.BUTTON3_DOWN_MASK
        }
        if (stateMask and SWT.SHIFT != 0) modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        if (stateMask and SWT.CONTROL != 0) modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        if (stateMask and SWT.ALT != 0) modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        if (stateMask and SWT.COMMAND != 0) modifiers = modifiers or InputEvent.META_DOWN_MASK
        return modifiers
    }

    class Factory : JcefOsrComponentFactory {
        override fun supports(framework: KromiumUiFramework): Boolean = framework == KromiumUiFramework.SWT

        override fun createComponent(browser: CefBrowserOsr, config: KromiumClientConfig?): JcefOsrComponent {
            return SwtOsrBuffer()
        }
    }

    companion object {
        init {
            registerFactory()
        }

        @JvmStatic
        fun registerFactory() {
            JcefOsrPanelFactory.register(KromiumUiFramework.SWT, Factory())
        }

        /**
         * Maps Chromium cursor types to native Eclipse SWT cursor constants.
         */
        @JvmStatic
        fun toSwtCursor(cursorType: Int): Int {
            return when (cursorType) {
                0 -> SWT.CURSOR_ARROW
                1 -> SWT.CURSOR_CROSS
                2 -> SWT.CURSOR_HAND
                3 -> SWT.CURSOR_IBEAM // I-beam pointer for text inputs
                4 -> SWT.CURSOR_WAIT
                5 -> SWT.CURSOR_HELP
                6, 13, 15 -> SWT.CURSOR_SIZEWE
                7, 10, 14 -> SWT.CURSOR_SIZENS
                8, 12, 16 -> SWT.CURSOR_SIZENESW
                9, 11, 17 -> SWT.CURSOR_SIZENWSE
                18, 21, 28 -> SWT.CURSOR_SIZEWE
                19, 22, 25 -> SWT.CURSOR_SIZENS
                20, 29 -> SWT.CURSOR_SIZEALL
                23, 27 -> SWT.CURSOR_SIZENESW
                24, 26 -> SWT.CURSOR_SIZENWSE
                30 -> SWT.CURSOR_IBEAM
                31 -> SWT.CURSOR_CROSS
                33, 37, 38, 39, 40 -> SWT.CURSOR_HAND
                34 -> SWT.CURSOR_APPSTARTING
                35, 36 -> SWT.CURSOR_NO
                else -> SWT.CURSOR_ARROW
            }
        }
    }

    /**
     * Native macOS Cocoa scroll delta resolver via reflection.
     * Extracts precise trackpad floating point deltas from NSEvent when running on macOS.
     */
    private object MacScrollHelper {
        private val isMac = System.getProperty("os.name").lowercase().contains("mac")
        private var sharedAppMethod: Method? = null
        private var currentEventMethod: Method? = null
        private var deltaYMethod: Method? = null
        private var deltaXMethod: Method? = null
        private var hasPreciseMethod: Method? = null
        private var scrollingDeltaYMethod: Method? = null
        private var scrollingDeltaXMethod: Method? = null
        var isSupported: Boolean = false
            private set

        init {
            if (isMac) {
                try {
                    val appCls = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication")
                    sharedAppMethod = appCls.getMethod("sharedApplication")
                    currentEventMethod = appCls.getMethod("currentEvent")

                    val eventCls = Class.forName("org.eclipse.swt.internal.cocoa.NSEvent")
                    deltaYMethod = runCatching { eventCls.getMethod("deltaY") }.getOrNull()
                    deltaXMethod = runCatching { eventCls.getMethod("deltaX") }.getOrNull()
                    hasPreciseMethod = runCatching { eventCls.getMethod("hasPreciseScrollingDeltas") }.getOrNull()
                    scrollingDeltaYMethod = runCatching { eventCls.getMethod("scrollingDeltaY") }.getOrNull()
                    scrollingDeltaXMethod = runCatching { eventCls.getMethod("scrollingDeltaX") }.getOrNull()
                    isSupported = sharedAppMethod != null && currentEventMethod != null
                } catch (_: Throwable) {
                    isSupported = false
                }
            }
        }

        class NativeScrollDelta(
            val isPrecise: Boolean,
            val delta: Double
        )

        fun getScrollDelta(horizontal: Boolean): NativeScrollDelta? {
            if (!isSupported) return null
            return try {
                val app = sharedAppMethod?.invoke(null) ?: return null
                val event = currentEventMethod?.invoke(app) ?: return null
                val isPrecise = hasPreciseMethod?.invoke(event) as? Boolean ?: false
                val delta = if (isPrecise) {
                    val method = if (horizontal) scrollingDeltaXMethod else scrollingDeltaYMethod
                    (method?.invoke(event) as? Number)?.toDouble() ?: 0.0
                } else {
                    val method = if (horizontal) deltaXMethod else deltaYMethod
                    (method?.invoke(event) as? Number)?.toDouble() ?: 0.0
                }
                if (delta != 0.0) NativeScrollDelta(isPrecise, delta) else null
            } catch (_: Throwable) {
                null
            }
        }
    }
}
