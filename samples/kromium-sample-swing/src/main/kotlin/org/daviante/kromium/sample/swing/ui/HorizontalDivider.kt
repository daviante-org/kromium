package org.daviante.kromium.sample.swing.ui

import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

class HorizontalDivider : JComponent() {

    init {
        isOpaque = false
        preferredSize = Dimension(1, 1)
        maximumSize = Dimension(Int.MAX_VALUE, 1)
        minimumSize = Dimension(1, 1)

        SwingTheme.addThemeListener {
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        g.color = SwingTheme.border
        g.drawLine(0, 0, width, 0)
    }
}
