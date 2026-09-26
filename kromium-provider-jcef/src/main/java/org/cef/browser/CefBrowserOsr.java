package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.handler.CefAcceleratedPaintInfo;

import org.daviante.kromium.api.config.KromiumClientConfig;
import org.daviante.kromium.api.error.KromiumException;
import org.daviante.kromium.api.ui.KromiumUiFramework;
import org.daviante.kromium.jcef.osr.JcefOsrComponent;
import org.daviante.kromium.jcef.osr.JcefOsrPanelFactory;
import org.daviante.kromium.jcef.ui.JcefShortcutHandler;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CefBrowserOsr extends CefBrowser_N implements CefRenderHandler {
    private Component canvas_;
    private JcefOsrComponent osrComponent_;
    private Object uiComponentObject_;
    private final KromiumUiFramework framework_;
    private boolean justCreated_ = false;
    private Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1);
    private Point screenPoint_ = new Point(0, 0);
    private double scaleFactor_ = detectDefaultScaleFactor();
    private boolean autoDetectScaleFactor_ = true;
    private double scrollMultiplier_ = 1.0;
    private double scrollRemainder_ = 0.0;
    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).contains("mac");
    private int depth = 32;
    private int depth_per_component = 8;
    private boolean isTransparent_;
    private CopyOnWriteArrayList<Consumer<CefPaintEvent>> onPaintListeners = new CopyOnWriteArrayList<>();
    private boolean frameRateConfigured_ = false;

    private void ensureFrameRate() {
        if (!frameRateConfigured_ && getNativeRef("CefBrowser") != 0) {
            try {
                setWindowlessFrameRate(60);
                frameRateConfigured_ = true;
            } catch (Throwable ignored) {
            }
        }
    }

    private static double detectDefaultScaleFactor() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();
        } catch (Throwable t) {
            return 1.0;
        }
    }

    public CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings) {
        this(client, url, transparent, context, null, null, settings, (KromiumClientConfig) null);
    }

    public CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings, KromiumUiFramework framework) {
        this(client, url, transparent, context, null, null, settings, framework != null ? KromiumClientConfig.builder().framework(framework).build() : null);
    }

    public CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings, KromiumClientConfig config) {
        this(client, url, transparent, context, null, null, settings, config);
    }

    private CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserOsr parent, Point inspectAt, CefBrowserSettings settings) {
        this(client, url, transparent, context, parent, inspectAt, settings, parent != null ? parent.framework_ : null);
    }

    private CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserOsr parent, Point inspectAt, CefBrowserSettings settings, KromiumUiFramework framework) {
        this(client, url, transparent, context, parent, inspectAt, settings, framework != null ? KromiumClientConfig.builder().framework(framework).build() : null);
    }

    private CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserOsr parent, Point inspectAt, CefBrowserSettings settings, KromiumClientConfig config) {
        super(client, url, context, parent, inspectAt, settings);
        this.isTransparent_ = transparent;
        this.framework_ = (config != null && config.getFramework() != null) ? config.getFramework() : (parent != null ? parent.framework_ : null);
        if (this.framework_ == null) {
            throw new KromiumException.FrameworkRequired();
        }

        if (this.framework_ == KromiumUiFramework.HEADLESS) {
            this.canvas_ = null;
            this.osrComponent_ = null;
            this.uiComponentObject_ = null;
            if (config != null) {
                this.browser_rect_.setBounds(0, 0, config.getInitialWidth(), config.getInitialHeight());
            }
        } else {
            JcefOsrComponent component = JcefOsrPanelFactory.createPanel(this.framework_, this, config);
            if (component != null) {
                setOsrComponent(component, component.getUiObject());
            }
        }

        if (this.canvas_ != null) {
            // Notify JCEF when the canvas size changes
            this.canvas_.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    browser_rect_.setBounds(0, 0, canvas_.getWidth(), canvas_.getHeight());
                    if (getNativeRef("CefBrowser") != 0) {
                        ensureFrameRate();
                        wasResized(canvas_.getWidth(), canvas_.getHeight());
                    }
                }
                @Override
                public void componentMoved(ComponentEvent e) {
                    if (canvas_.isShowing()) {
                        screenPoint_ = canvas_.getLocationOnScreen();
                    }
                }
            });

            // Forward Focus
            this.canvas_.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { setFocus(true); }
                @Override
                public void focusLost(FocusEvent e) { setFocus(false); }
            });

            // Forward Mouse Events
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setFocus(true);
                    if (!canvas_.hasFocus()) {
                        canvas_.requestFocusInWindow();
                    }
                    sendMouseEvent(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseEntered(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseExited(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseClicked(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseMoved(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseDragged(MouseEvent e) { sendMouseEvent(e); }
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    ensureFrameRate();
                    double rotation = e.getPreciseWheelRotation();
                    if (rotation == 0.0) {
                        rotation = e.getWheelRotation();
                    }
                    int scrollAmount = Math.max(1, e.getScrollAmount());
                    boolean isDiscrete = Math.abs(rotation) >= 1.0 && Math.floor(Math.abs(rotation)) == Math.abs(rotation);
                    double basePixelsPerUnit = isDiscrete ? 100.0 : (IS_MAC ? 20.0 : 28.0);
                    double totalDelta = (rotation * scrollAmount * basePixelsPerUnit * scrollMultiplier_) + scrollRemainder_;
                    int deltaPixels = (int) totalDelta;
                    scrollRemainder_ = totalDelta - deltaPixels;

                    if (deltaPixels != 0) {
                        MouseWheelEvent smoothedEvent = new MouseWheelEvent(
                            (Component) e.getSource(),
                            e.getID(),
                            e.getWhen(),
                            e.getModifiersEx(),
                            e.getX(),
                            e.getY(),
                            e.getClickCount(),
                            e.isPopupTrigger(),
                            MouseWheelEvent.WHEEL_UNIT_SCROLL,
                            1,
                            deltaPixels
                        );
                        sendMouseWheelEvent(smoothedEvent);
                    }
                }
            };
            this.canvas_.addMouseListener(mouseAdapter);
            this.canvas_.addMouseMotionListener(mouseAdapter);
            this.canvas_.addMouseWheelListener(mouseAdapter);

            // Forward Keyboard Events and handle system shortcuts in OSR mode via JcefShortcutHandler
            this.canvas_.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (JcefShortcutHandler.handleAwtKeyEvent(CefBrowserOsr.this, e)) {
                        return;
                    }
                    sendKeyEvent(e);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (JcefShortcutHandler.handleAwtKeyEvent(CefBrowserOsr.this, e)) {
                        return;
                    }
                    sendKeyEvent(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (JcefShortcutHandler.handleAwtKeyEvent(CefBrowserOsr.this, e)) {
                        return;
                    }
                    sendKeyEvent(e);
                }
            });

            this.canvas_.setFocusable(true);
            if (this.canvas_ instanceof javax.swing.JComponent) {
                ((javax.swing.JComponent) this.canvas_).setRequestFocusEnabled(true);
            }

            this.canvas_.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && canvas_.isShowing()) {
                        int w = canvas_.getWidth();
                        int h = canvas_.getHeight();
                        if (w > 0 && h > 0) {
                            browser_rect_.setBounds(0, 0, w, h);
                            if (getNativeRef("CefBrowser") != 0) {
                                ensureFrameRate();
                                wasResized(w, h);
                            }
                        }
                        try {
                            screenPoint_ = canvas_.getLocationOnScreen();
                        } catch (Throwable ignored) {}
                    }
                }
            });
        }

        // Start native CEF browser immediately for all frameworks (AWT, Swing, SWT, JavaFX, headless)
        createImmediately();
    }

    public void setScaleFactor(double factor) {
        if (factor > 0.0 && (this.scaleFactor_ != factor || this.autoDetectScaleFactor_)) {
            this.scaleFactor_ = factor;
            this.autoDetectScaleFactor_ = false;
            notifyScreenInfoChanged();
            int w = canvas_ != null && canvas_.getWidth() > 0 ? canvas_.getWidth() : browser_rect_.width;
            int h = canvas_ != null && canvas_.getHeight() > 0 ? canvas_.getHeight() : browser_rect_.height;
            if (w > 0 && h > 0) {
                wasResized(w, h);
            }
        }
    }

    public void setAutoDetectScaleFactor(boolean autoDetect) {
        if (this.autoDetectScaleFactor_ != autoDetect) {
            this.autoDetectScaleFactor_ = autoDetect;
            if (autoDetect) {
                this.scaleFactor_ = detectDefaultScaleFactor();
                notifyScreenInfoChanged();
                int w = canvas_ != null && canvas_.getWidth() > 0 ? canvas_.getWidth() : browser_rect_.width;
                int h = canvas_ != null && canvas_.getHeight() > 0 ? canvas_.getHeight() : browser_rect_.height;
                if (w > 0 && h > 0) {
                    wasResized(w, h);
                }
            }
        }
    }

    public boolean isAutoDetectScaleFactor() {
        return autoDetectScaleFactor_;
    }

    public double getScaleFactor() {
        return scaleFactor_;
    }

    public JcefOsrComponent getOsrComponent() {
        return osrComponent_;
    }

    public void setOsrComponent(JcefOsrComponent component, Object uiComponent) {
        this.osrComponent_ = component;
        this.uiComponentObject_ = uiComponent != null ? uiComponent : component;
        if (component instanceof Component) {
            this.canvas_ = (Component) component;
        }
    }

    public Object getUIComponentObject() {
        return uiComponentObject_ != null ? uiComponentObject_ : canvas_;
    }

    public KromiumUiFramework getUiFramework() {
        return framework_;
    }

    public void notifyResized(int width, int height) {
        browser_rect_.setBounds(0, 0, Math.max(1, width), Math.max(1, height));
        if (getNativeRef("CefBrowser") != 0) {
            ensureFrameRate();
            wasResized(Math.max(1, width), Math.max(1, height));
        }
    }

    public void setScreenPoint(Point pt) {
        if (pt != null) {
            this.screenPoint_ = new Point(pt);
        }
    }

    public void setScrollMultiplier(double multiplier) {
        if (multiplier > 0.0) {
            this.scrollMultiplier_ = multiplier;
        }
    }

    public double getScrollMultiplier() {
        return scrollMultiplier_;
    }

    @Override
    public void createImmediately() {
        justCreated_ = true;
        createBrowserIfRequired(false);
    }

    @Override
    public Component getUIComponent() {
        return canvas_;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser parent, Point inspectAt) {
        return new CefBrowserOsr(client, url, isTransparent_, context, (CefBrowserOsr) this, inspectAt, null, framework_);
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        int w = canvas_ != null && canvas_.getWidth() > 0 ? canvas_.getWidth() : browser_rect_.width;
        int h = canvas_ != null && canvas_.getHeight() > 0 ? canvas_.getHeight() : browser_rect_.height;
        return new Rectangle(0, 0, Math.max(1, w), Math.max(1, h));
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point p = new Point(screenPoint_);
        p.translate(viewPoint.x, viewPoint.y);
        return p;
    }

    @Override
    public double getDeviceScaleFactor(CefBrowser browser) {
        return scaleFactor_;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (osrComponent_ != null) {
            osrComponent_.setPopupVisible(show);
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        if (osrComponent_ != null) {
            osrComponent_.setPopupBounds(size);
        }
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.add(listener);
    }

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.clear();
        onPaintListeners.add(listener);
    }

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.remove(listener);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (osrComponent_ != null) {
            osrComponent_.onPaint(buffer, width, height, popup);
        }
        
        if (!onPaintListeners.isEmpty()) {
            CefPaintEvent event = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
            for (Consumer<CefPaintEvent> listener : onPaintListeners) {
                listener.accept(event);
            }
        }
    }

    @Override
    public void onAcceleratedPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, CefAcceleratedPaintInfo sharedHandle) {}

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        if (osrComponent_ != null) {
            osrComponent_.onCursorChange(cursorType);
        } else if (canvas_ != null) {
            updateAwtCursor(canvas_, cursorType);
        }
        return true;
    }

    private void updateAwtCursor(Component comp, int cursorType) {
        Cursor c = mapCefCursorToAwt(cursorType);
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            comp.setCursor(c);
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> comp.setCursor(c));
        }
    }

    public static Cursor mapCefCursorToAwt(int cursorType) {
        int awtType = switch (cursorType) {
            case 0 -> Cursor.DEFAULT_CURSOR;
            case 1, 31 -> Cursor.CROSSHAIR_CURSOR;
            case 2, 33, 37, 38, 39, 40 -> Cursor.HAND_CURSOR;
            case 3, 30 -> Cursor.TEXT_CURSOR; // I-beam cursor for text inputs / vertical text
            case 4, 34 -> Cursor.WAIT_CURSOR;
            case 6, 13, 15, 18, 21, 28 -> Cursor.E_RESIZE_CURSOR;
            case 7, 10, 14, 19, 22, 25 -> Cursor.N_RESIZE_CURSOR;
            case 8, 12, 16, 23, 27 -> Cursor.NE_RESIZE_CURSOR;
            case 9, 11, 17, 24, 26 -> Cursor.NW_RESIZE_CURSOR;
            case 20, 29 -> Cursor.MOVE_CURSOR;
            default -> Cursor.DEFAULT_CURSOR;
        };
        return Cursor.getPredefinedCursor(awtType);
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {}

    private void createBrowserIfRequired(boolean hasParent) {
        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), 0, true, isTransparent_, null, getInspectAt());
            } else {
                createBrowser(getClient(), 0, getUrl(), true, isTransparent_, null);
            }
        } else if (hasParent && justCreated_) {
            notifyAfterParentChanged();
            setFocus(true);
            justCreated_ = false;
        }
    }

    @Override
    public void setFocus(boolean focused) {
        if (focused) {
            ensureFrameRate();
        }
        super.setFocus(focused);
    }

    private void notifyAfterParentChanged() {
        getClient().onAfterParentChanged(this);
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        screenInfo.Set(scaleFactor_, depth, depth_per_component, false, browser_rect_.getBounds(), browser_rect_.getBounds());
        return true;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isWindowless() {
        return true;
    }
}