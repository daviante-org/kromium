package org.daviante.kromium.sample.swing.ui

import java.awt.BasicStroke
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class KromiumSwitch(
    checked: Boolean = false
) : JComponent() {

    var isChecked: Boolean = checked
        set(value) {
            field = value
            repaint()
        }

    var onCheckedChanged: ((Boolean) -> Unit)? = null

    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (isEnabled) {
                    isChecked = !isChecked
                    onCheckedChanged?.invoke(isChecked)
                }
            }
        })

        SwingTheme.addThemeListener {
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = 38
        val h = 20
        val x = (width - w) / 2
        val y = (height - h) / 2

        if (isChecked) {
            g2.color = SwingTheme.primary
            g2.fillRoundRect(x, y, w, h, h, h)

            val knobSize = 14
            val knobX = x + w - knobSize - 3
            val knobY = y + (h - knobSize) / 2
            g2.color = SwingTheme.onPrimary
            g2.fillOval(knobX, knobY, knobSize, knobSize)
        } else {
            g2.color = SwingTheme.surfaceVariant
            g2.fillRoundRect(x, y, w, h, h, h)

            g2.color = SwingTheme.border
            g2.stroke = BasicStroke(1f)
            g2.drawRoundRect(x, y, w - 1, h - 1, h, h)

            val knobSize = 14
            val knobX = x + 3
            val knobY = y + (h - knobSize) / 2
            g2.color = SwingTheme.textSecondary
            g2.fillOval(knobX, knobY, knobSize, knobSize)
        }

        g2.dispose()
    }

    override fun getPreferredSize(): Dimension = Dimension(42, 24)
    override fun getMinimumSize(): Dimension = Dimension(42, 24)
}
