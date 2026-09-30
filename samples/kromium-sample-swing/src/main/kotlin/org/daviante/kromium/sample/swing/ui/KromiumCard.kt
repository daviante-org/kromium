package org.daviante.kromium.sample.swing.ui

import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class KromiumCard(
    layout: LayoutManager? = null,
    private val padding: Int = 10
) : JPanel() {

    init {
        if (layout != null) {
            this.layout = layout
        }
        isOpaque = false
        border = EmptyBorder(padding, padding, padding, padding)

        SwingTheme.addThemeListener {
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width
        val h = height
        val arc = 8

        g2.color = SwingTheme.surfaceVariant
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

        g2.color = SwingTheme.border
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

        g2.dispose()
        super.paintComponent(g)
    }
}
