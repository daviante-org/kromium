package org.daviante.kromium.jcef.osr;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reusable, high-performance Java2D double-buffered pixel renderer for OSR surfaces.
 * Shared across Swing, AWT, and Compose to eliminate redundant buffer and blitting code.
 */
public class JcefJava2dRenderer {

    private final Component repaintTarget;
    private final Object bufferLock = new Object();

    private BufferedImage frontBuffer;
    private BufferedImage backBuffer;
    private int[] backBufferData;

    private BufferedImage popupBuffer;
    private int[] popupBufferData;
    private Rectangle popupRect;
    private volatile boolean isPopupVisible = false;

    private volatile int bufferedImageType = BufferedImage.TYPE_INT_ARGB_PRE;
    private volatile ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private final Map<RenderingHints.Key, Object> customRenderingHints = new ConcurrentHashMap<>();

    public JcefJava2dRenderer(Component repaintTarget) {
        this.repaintTarget = repaintTarget;
    }

    public boolean hasFrame() {
        synchronized (bufferLock) {
            return frontBuffer != null;
        }
    }

    public int getBufferedImageType() {
        return bufferedImageType;
    }

    public void setBufferedImageType(int type) {
        if (this.bufferedImageType != type) {
            this.bufferedImageType = type;
            synchronized (bufferLock) {
                frontBuffer = null;
                backBuffer = null;
                popupBuffer = null;
            }
            repaintTarget.repaint();
        }
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        repaintTarget.repaint();
    }

    public void setRenderingHint(RenderingHints.Key key, Object value) {
        if (value != null) {
            customRenderingHints.put(key, value);
        } else {
            customRenderingHints.remove(key);
        }
        repaintTarget.repaint();
    }

    public Object getRenderingHint(RenderingHints.Key key) {
        return customRenderingHints.get(key);
    }

    public void setInterpolation(Object interpolationHint) {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
    }

    public void onPaint(ByteBuffer buffer, int width, int height, boolean isPopup) {
        if (isPopup) {
            handlePopupPaint(buffer, width, height);
            return;
        }

        if (backBuffer == null || backBuffer.getWidth() != width || backBuffer.getHeight() != height || backBuffer.getType() != bufferedImageType) {
            BufferedImage newBack = new BufferedImage(width, height, bufferedImageType);
            backBuffer = newBack;
            backBufferData = ((DataBufferInt) newBack.getRaster().getDataBuffer()).getData();
        }

        IntBuffer intBuffer = buffer.order(byteOrder).asIntBuffer();
        int[] data = backBufferData;
        if (data == null) return;
        int pixelsToCopy = Math.min(intBuffer.remaining(), data.length);
        intBuffer.get(data, 0, pixelsToCopy);

        synchronized (bufferLock) {
            BufferedImage temp = frontBuffer;
            frontBuffer = backBuffer;
            backBuffer = temp;
            if (backBuffer != null) {
                backBufferData = ((DataBufferInt) backBuffer.getRaster().getDataBuffer()).getData();
            }
        }

        repaintTarget.repaint();
    }

    private void handlePopupPaint(ByteBuffer buffer, int width, int height) {
        synchronized (bufferLock) {
            if (popupBuffer == null || popupBuffer.getWidth() != width || popupBuffer.getHeight() != height || popupBuffer.getType() != bufferedImageType) {
                BufferedImage newPopup = new BufferedImage(width, height, bufferedImageType);
                popupBuffer = newPopup;
                popupBufferData = ((DataBufferInt) newPopup.getRaster().getDataBuffer()).getData();
            }
            IntBuffer intBuffer = buffer.order(byteOrder).asIntBuffer();
            int[] data = popupBufferData;
            if (data == null) return;
            int pixelsToCopy = Math.min(intBuffer.remaining(), data.length);
            intBuffer.get(data, 0, pixelsToCopy);
        }
        repaintTarget.repaint();
    }

    public void setPopupBounds(Rectangle rect) {
        synchronized (bufferLock) {
            this.popupRect = rect;
        }
    }

    public void setPopupVisible(boolean visible) {
        this.isPopupVisible = visible;
        repaintTarget.repaint();
    }

    public void render(Graphics g, int componentWidth, int componentHeight) {
        synchronized (bufferLock) {
            BufferedImage front = frontBuffer;
            if (front != null) {
                if (g instanceof Graphics2D) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                    for (Map.Entry<RenderingHints.Key, Object> entry : customRenderingHints.entrySet()) {
                        g2d.setRenderingHint(entry.getKey(), entry.getValue());
                    }

                    AffineTransform oldTx = g2d.getTransform();
                    double scaleX = oldTx.getScaleX();
                    double scaleY = oldTx.getScaleY();
                    if (scaleX > 1.0 && front.getWidth() > componentWidth) {
                        AffineTransform unscaledTx = new AffineTransform(oldTx);
                        unscaledTx.scale(1.0 / scaleX, 1.0 / scaleY);
                        g2d.setTransform(unscaledTx);
                        g2d.drawImage(front, 0, 0, null);
                        g2d.setTransform(oldTx);
                    } else {
                        g2d.drawImage(front, 0, 0, componentWidth, componentHeight, null);
                    }
                } else {
                    g.drawImage(front, 0, 0, componentWidth, componentHeight, null);
                }
            }

            BufferedImage popup = popupBuffer;
            Rectangle rect = popupRect;
            if (isPopupVisible && popup != null && rect != null) {
                if (g instanceof Graphics2D) {
                    Graphics2D g2d = (Graphics2D) g;
                    AffineTransform oldTx = g2d.getTransform();
                    double scaleX = oldTx.getScaleX();
                    double scaleY = oldTx.getScaleY();
                    if (scaleX > 1.0 && popup.getWidth() > rect.width) {
                        AffineTransform unscaledTx = new AffineTransform(oldTx);
                        unscaledTx.scale(1.0 / scaleX, 1.0 / scaleY);
                        g2d.setTransform(unscaledTx);
                        int px = (int) Math.round(rect.x * scaleX);
                        int py = (int) Math.round(rect.y * scaleY);
                        g2d.drawImage(popup, px, py, null);
                        g2d.setTransform(oldTx);
                    } else {
                        g2d.drawImage(popup, rect.x, rect.y, rect.width, rect.height, null);
                    }
                } else {
                    g.drawImage(popup, rect.x, rect.y, rect.width, rect.height, null);
                }
            }
        }
    }
}
