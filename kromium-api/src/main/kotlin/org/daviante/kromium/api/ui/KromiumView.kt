package org.daviante.kromium.api.ui

import org.daviante.kromium.api.print.KromiumPdfSettings
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture

/**
 * Facade for rendering, zooming, and visual operations.
 */
interface KromiumView {
    /**
     * The native rendering surface associated with this browser.
     * Can be adapted to UI toolkits using the Adapter pattern.
     */
    val surface: KromiumRenderSurface

    /**
     * Gets or sets the current zoom level. (0.0 is 100%, 1.0 is ~120%, -1.0 is ~80%).
     */
    var zoomLevel: Double

    /**
     * Sets the zoom level for this browser. (0.0 is 100%, 1.0 is ~120%, -1.0 is ~80%).
     */
    fun setZoom(level: Double)

    /**
     * Gets the current zoom level.
     */
    fun getZoom(): Double

    /**
     * Captures a screenshot of the current page rendering synchronously.
     * Note: Prefer [takeScreenshotAsync] where available for non-blocking native capture.
     */
    fun takeScreenshot(): BufferedImage?
    
    /**
     * Asynchronously captures a screenshot of the current page using the engine's native capabilities.
     * This method is generally faster and doesn't rely on OS window visibility.
     */
    fun takeScreenshotAsync(): CompletableFuture<BufferedImage?>

    /**
     * Toggles graphics antialiasing on the underlying rendering component.
     */
    fun setAntialiasing(enabled: Boolean)
    
    /**
     * Sets the rendering interpolation quality.
     * Common values: "high", "low", "bilinear", "bicubic", "nearest_neighbor"
     */
    fun setInterpolation(quality: String)
    
    /**
     * Gets or sets the manual UI scaling factor (e.g. 1.0 = 100%, 2.0 = 200%).
     */
    var scaleFactor: Double

    /**
     * Whether OSR automatically detects display DPI scaling (e.g. 2.0 on Retina, 1.25/1.5 on Windows).
     */
    var isAutoDetectScaleFactor: Boolean

    /**
     * Scroll sensitivity multiplier for OSR mode (default: 1.0).
     */
    var scrollMultiplier: Double

    /**
     * Resets the UI scaling factor to automatically match the system DPI.
     */
    fun resetScaleFactorToAuto()

    /**
     * Triggers the operating system's native interactive print dialog for the current page.
     */
    fun print()

    /**
     * Generates a PDF of the current page and saves it to the specified path using default settings.
     *
     * @param path The absolute or relative file path to save the PDF.
     * @return A CompletableFuture that completes with true if successful.
     */
    fun printToPdf(path: String): CompletableFuture<Boolean> = printToPdf(path, KromiumPdfSettings.Default)

    /**
     * Generates a vector PDF of the current page and saves it to the specified path with custom settings.
     *
     * @param path The absolute or relative file path to save the PDF.
     * @param settings Detailed PDF formatting, paper sizing, and margin options.
     * @return A CompletableFuture that completes with true if successful.
     */
    fun printToPdf(path: String, settings: KromiumPdfSettings): CompletableFuture<Boolean>

    /**
     * Dispatches a standardized cross-platform keyboard event into the browser viewport.
     * Handles RawKeyDown, Char, and KeyUp sequences, shortcut execution, and Blink caret editing.
     *
     * @param event The unified keyboard event to dispatch.
     */
    fun sendKeyEvent(event: KromiumKeyEvent)

    /**
     * Explicitly notifies the underlying browser rendering engine whether this view has active focus.
     *
     * @param focused true if the browser host should receive focus, false otherwise.
     */
    fun setFocus(focused: Boolean = true)

    /**
     * Resizes the underlying browser rendering surface to the specified dimensions.
     * Useful for dynamically resizing headless viewports.
     */
    fun resize(width: Int, height: Int)
}
