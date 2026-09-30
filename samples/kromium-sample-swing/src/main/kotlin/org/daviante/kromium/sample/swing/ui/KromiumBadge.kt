package org.daviante.kromium.sample.swing.ui

import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

class KromiumBadge(
    var text: String = "",
    var isActive: Boolean = false
) : JComponent() {

    init {
        isOpaque = false
        SwingTheme.addThemeListener {
            repaint()
        }
    }

    fun setState(text: String, active: Boolean) {
        this.text = text
        this.isActive = active
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val w = width
        val h = height
        val arc = 6

        val bg = if (isActive) {
            SwingTheme.surfaceVariant
        } else {
            SwingTheme.surface
        }
        g2.color = bg
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

        val strokeColor = if (isActive) SwingTheme.primary else SwingTheme.border
        g2.color = strokeColor
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

        g2.color = if (isActive) SwingTheme.primary else SwingTheme.textSecondary
        g2.font = SwingTheme.fontMonospace(10f, bold = true)

        val fm = g2.fontMetrics
        val tx = (w - fm.stringWidth(text)) / 2
        val ty = (h + fm.ascent) / 2 - 2
        g2.drawString(text, tx, ty)

        g2.dispose()
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(SwingTheme.fontMonospace(10f, bold = true))
        val textWidth = fm.stringWidth(text)
        return Dimension(textWidth + 14, 22)
    }
}
