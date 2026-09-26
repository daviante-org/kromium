package org.daviante.kromium.sample.swing.ui

import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.border.EmptyBorder

class KromiumTextField(
    var placeholder: String = "",
    var isMonospace: Boolean = true,
    var fontSize: Float = 12f
) : JTextField() {

    var onEnter: (() -> Unit)? = null

    init {
        isOpaque = false
        border = EmptyBorder(6, 10, 6, 10)

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                repaint()
            }

            override fun focusLost(e: FocusEvent) {
                repaint()
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    onEnter?.invoke()
                }
            }
        })

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
    }

    private fun updateColors() {
        font = if (isMonospace) {
            SwingTheme.fontMonospace(fontSize, false)
        } else {
            SwingTheme.fontSans(fontSize, false)
        }
        foreground = SwingTheme.textPrimary
        caretColor = SwingTheme.primary
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val w = width
        val h = height
        val arc = 6

        g2.color = SwingTheme.surfaceVariant
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

        val borderColor = if (hasFocus()) SwingTheme.primary else SwingTheme.border
        g2.color = borderColor
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

        super.paintComponent(g)

        if (text.isEmpty() && placeholder.isNotEmpty()) {
            g2.color = SwingTheme.textSecondary
            g2.font = font
            val insets: Insets = insets
            val fm = g2.fontMetrics
            val y = (h - fm.height) / 2 + fm.ascent
            g2.drawString(placeholder, insets.left, y)
        }

        g2.dispose()
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        size.height = maxOf(size.height, 34)
        return size
    }
}
