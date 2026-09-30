package org.daviante.kromium.jcef.osr;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.nio.ByteBuffer;

/**
 * Framework-neutral interface for any component or surface that renders JCEF OSR pixel buffers.
 * Implemented by framework-specific panels across Swing, AWT, JavaFX, SWT, and Compose.
 */
public interface JcefOsrComponent {

    /**
     * Called when a new video frame or popup is rendered by Chromium.
     *
     * @param buffer  Direct byte buffer containing BGRA pixel data
     * @param width   Frame width in pixels
     * @param height  Frame height in pixels
     * @param isPopup True if this paint call is for an open popup menu/dropdown
     */
    void onPaint(ByteBuffer buffer, int width, int height, boolean isPopup);

    /**
     * Updates the bounding rectangle of the active popup dropdown.
     */
    void setPopupBounds(Rectangle rect);

    /**
     * Toggles visibility of the active popup dropdown.
     */
    void setPopupVisible(boolean visible);

    /**
     * Called when Chromium changes the mouse cursor over a web element (e.g. I-beam for text inputs).
     */
    default void onCursorChange(int cursorType) {}

    /**
     * Returns the underlying java.awt.Component if this is an AWT/Swing component, or null otherwise.
     */
    default Component getComponent() {
        return null;
    }

    /**
     * Returns the framework-native UI object (e.g. JPanel, Canvas, JavaFX Node, SWT Canvas).
     */
    default Object getUiObject() {
        return getComponent();
    }

    /**
     * Optionally sets a rendering hint on the OSR component.
     */
    default void setRenderingHint(RenderingHints.Key key, Object value) {}

    /**
     * Optionally configures the interpolation quality hint.
     */
    default void setInterpolation(Object interpolationHint) {}

    /**
     * Requests native framework window/component focus for this OSR canvas.
     */
    default void requestFocus() {}
}
