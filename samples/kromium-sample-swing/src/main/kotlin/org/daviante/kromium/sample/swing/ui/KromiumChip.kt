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

class KromiumChip(
    text: String = "",
    selected: Boolean = false,
    var fontSize: Float = 10f
) : JButton(text) {

    private var isHovered = false

    init {
        this.isSelected = selected
        isContentAreaFilled = false
        isFocusPainted = false
        isBorderPainted = false
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                isHovered = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                isHovered = false
                repaint()
            }
        })

        SwingTheme.addThemeListener {
            repaint()
        }
    }

    override fun setSelected(b: Boolean) {
        super.setSelected(b)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val w = width
        val h = height
        val arc = 6

        val bg = if (isSelected) {
            SwingTheme.surfaceVariant
        } else if (isHovered) {
            SwingTheme.surfaceVariant
        } else {
            SwingTheme.surface
        }
        g2.color = bg
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

        val borderColor = if (isSelected) {
            SwingTheme.primary
        } else if (isHovered) {
            SwingTheme.secondary
        } else {
            SwingTheme.border
        }
        g2.color = borderColor
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

        g2.color = if (isSelected) SwingTheme.primary else SwingTheme.textPrimary
        g2.font = SwingTheme.fontSans(fontSize, bold = isSelected)

        val fm = g2.fontMetrics
        val textWidth = fm.stringWidth(text)
        val textHeight = fm.ascent

        val x = (w - textWidth) / 2
        val y = (h + textHeight) / 2 - 2
        g2.drawString(text, x, y)

        g2.dispose()
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(SwingTheme.fontSans(fontSize, false))
        val textWidth = fm.stringWidth(text)
        return Dimension(maxOf(textWidth + 16, 44), 28)
    }
}
