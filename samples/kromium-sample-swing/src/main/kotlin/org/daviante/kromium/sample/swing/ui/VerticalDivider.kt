package org.daviante.kromium.sample.swing.ui

import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

class VerticalDivider(
    private val dividerHeight: Int = 20
) : JComponent() {

    init {
        isOpaque = false
        preferredSize = Dimension(1, dividerHeight)
        maximumSize = Dimension(1, dividerHeight)
        minimumSize = Dimension(1, dividerHeight)

        SwingTheme.addThemeListener {
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        g.color = SwingTheme.border
        g.drawLine(0, 0, 0, height)
    }
}
