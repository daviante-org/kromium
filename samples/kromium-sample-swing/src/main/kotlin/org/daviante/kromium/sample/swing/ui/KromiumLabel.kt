package org.daviante.kromium.sample.swing.ui

import javax.swing.JLabel

class KromiumLabel(
    text: String = "",
    var isMuted: Boolean = false,
    var isBold: Boolean = false,
    var isMonospace: Boolean = false,
    var fontSize: Float = 12f
) : JLabel(text) {

    init {
        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
    }

    private fun updateColors() {
        foreground = if (isMuted) SwingTheme.textSecondary else SwingTheme.textPrimary
        font = if (isMonospace) {
            SwingTheme.fontMonospace(fontSize, isBold)
        } else {
            SwingTheme.fontSans(fontSize, isBold)
        }
        repaint()
    }
}
