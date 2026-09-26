package org.daviante.kromium.sample.swing.ui

import java.awt.BasicStroke
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

class KromiumButton(
    text: String = "",
    var isPrimary: Boolean = false,
    var fontSize: Float = 11f,
    var isMonospace: Boolean = false
) : JButton(text) {

    private var isHovered = false
    private var isPressed = false

    init {
        isContentAreaFilled = false
        isFocusPainted = false
        isFocusable = false
        isBorderPainted = false
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                if (isEnabled) {
                    isHovered = true
                    repaint()
                }
            }

            override fun mouseExited(e: MouseEvent) {
                isHovered = false
                isPressed = false
                repaint()
            }

            override fun mousePressed(e: MouseEvent) {
                if (isEnabled) {
                    isPressed = true
                    repaint()
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                isPressed = false
                repaint()
            }
        })

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
    }

    private fun updateColors() {
        font = if (isMonospace) {
            SwingTheme.fontMonospace(fontSize, isPrimary)
        } else {
            SwingTheme.fontSans(fontSize, isPrimary)
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val w = width
        val h = height
        val arc = 6

        if (isPrimary) {
            val bg = if (!isEnabled) {
                SwingTheme.surfaceVariant
            } else if (isPressed) {
                SwingTheme.secondary
            } else if (isHovered) {
                SwingTheme.secondary
            } else {
                SwingTheme.primary
            }

            g2.color = bg
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

            g2.color = if (!isEnabled) SwingTheme.textSecondary else SwingTheme.onPrimary
        } else {
            val bg = if (!isEnabled) {
                SwingTheme.surfaceVariant
            } else if (isPressed) {
                SwingTheme.surfaceVariant
            } else if (isHovered) {
                SwingTheme.surfaceVariant
            } else {
                SwingTheme.surface
            }

            g2.color = bg
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

            val borderColor = if (!isEnabled) {
                SwingTheme.border
            } else if (isHovered) {
                SwingTheme.primary
            } else {
                SwingTheme.border
            }

            g2.color = borderColor
            g2.stroke = BasicStroke(1f)
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

            g2.color = if (!isEnabled) SwingTheme.textSecondary else SwingTheme.textPrimary
        }

        g2.font = font
        val fm = g2.fontMetrics
        val textWidth = fm.stringWidth(text)
        val textHeight = fm.ascent

        val x = (w - textWidth) / 2
        val y = (h + textHeight) / 2 - 2
        g2.drawString(text, x, y)

        g2.dispose()
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        size.height = maxOf(size.height, 32)
        size.width = maxOf(size.width + 16, 48)
        return size
    }
}
