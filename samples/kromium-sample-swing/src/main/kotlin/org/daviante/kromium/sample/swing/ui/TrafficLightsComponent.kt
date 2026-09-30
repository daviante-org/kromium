package org.daviante.kromium.sample.swing.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

class TrafficLightsComponent : JComponent() {

    private val red = Color(0xFF, 0x5F, 0x56)
    private val yellow = Color(0xFF, 0xBD, 0x2E)
    private val green = Color(0x27, 0xC9, 0x3F)

    init {
        isOpaque = false
        preferredSize = Dimension(52, 14)
        minimumSize = Dimension(52, 14)
        maximumSize = Dimension(52, 14)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val size = 11
        val y = (height - size) / 2

        g2.color = red
        g2.fillOval(0, y, size, size)

        g2.color = yellow
        g2.fillOval(16, y, size, size)

        g2.color = green
        g2.fillOval(32, y, size, size)

        g2.dispose()
    }
}
