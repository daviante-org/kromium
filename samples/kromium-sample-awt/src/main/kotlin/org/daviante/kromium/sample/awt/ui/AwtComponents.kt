package org.daviante.kromium.sample.awt.ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JTextArea
import javax.swing.JTextField

/**
 * Modern AWT Panel that explicitly paints its background in Java2D.
 * Eliminates unpainted/transparent background regions and OS erase dependencies across all platforms.
 */
open class AwtPanel(layout: LayoutManager = FlowLayout()) : Panel(layout) {
    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        val bg = background
        if (bg != null) {
            g.color = bg
            g.fillRect(0, 0, width, height)
        }
        super.paint(g)
    }
}

/**
 * Lightweight, peer-free AWT Button that renders 100% faithful to [AwtTheme] colors
 * on all operating systems (especially macOS Aqua where native [java.awt.Button] peers
 * ignore custom background colors and force light-theme styling).
 */
class AwtButton(
    initialText: String = "",
    var isPrimary: Boolean = false,
    var onClick: () -> Unit = {}
) : Component() {

    var text: String = initialText
        set(value) {
            field = value
            invalidate()
            repaint()
        }

    private var isHovered = false
    private var isPressed = false

    init {
        font = AwtTheme.bodyFont
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
                if (isEnabled && isPressed && isHovered) {
                    onClick()
                }
                isPressed = false
                repaint()
            }
        })
    }

    fun addActionListener(action: () -> Unit) {
        val prev = onClick
        onClick = { prev(); action() }
    }

    override fun setEnabled(b: Boolean) {
        super.setEnabled(b)
        cursor = if (b) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
        repaint()
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font ?: AwtTheme.bodyFont)
        val textWidth = fm.stringWidth(text)
        val textHeight = fm.height
        return Dimension(textWidth + 18, textHeight + 10)
    }

    override fun getMinimumSize(): Dimension = preferredSize

    override fun paint(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Background color
        val bg = when {
            !isEnabled -> AwtTheme.surfaceVariant
            isPressed -> if (isPrimary) AwtTheme.primary.darker() else AwtTheme.surfaceVariant.darker()
            isHovered -> if (isPrimary) AwtTheme.primary.brighter() else AwtTheme.surfaceVariant.brighter()
            isPrimary -> AwtTheme.primary
            else -> AwtTheme.surfaceVariant
        }
        g2.color = bg
        g2.fillRoundRect(0, 0, width, height, 6, 6)

        // Subtle Border
        g2.color = if (isPrimary) AwtTheme.primary else AwtTheme.border
        g2.drawRoundRect(0, 0, width - 1, height - 1, 6, 6)

        // Centered Text
        val fg = when {
            !isEnabled -> AwtTheme.textSecondary
            isPrimary -> Color.WHITE
            else -> AwtTheme.textPrimary
        }
        g2.color = fg
        g2.font = font ?: AwtTheme.bodyFont
        val fm = g2.fontMetrics
        val textX = (width - fm.stringWidth(text)) / 2
        val textY = (height - fm.height) / 2 + fm.ascent
        g2.drawString(text, textX, textY)

        g2.dispose()
    }
}

/**
 * Lightweight, peer-free AWT Label with transparent background and antialiased typography.
 * Resolves macOS opaque gray rectangular patches caused by native Cocoa label peers.
 */
class AwtLabel(
    initialText: String = "",
    var isBold: Boolean = false,
    var customColor: Color? = null
) : Component() {

    var text: String = initialText
        set(value) {
            field = value
            invalidate()
            repaint()
        }

    init {
        font = if (isBold) AwtTheme.titleFont else AwtTheme.bodyFont
        if (customColor != null) {
            foreground = customColor
        }
    }

    override fun setForeground(c: Color?) {
        super.setForeground(c)
        customColor = c
        repaint()
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(getLabelFont())
        return Dimension(fm.stringWidth(text) + 6, fm.height + 4)
    }

    override fun getMinimumSize(): Dimension = preferredSize

    private fun getLabelFont(): Font = if (isBold) AwtTheme.titleFont else (font ?: AwtTheme.bodyFont)

    override fun paint(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.color = customColor ?: foreground ?: AwtTheme.textPrimary
        g2.font = getLabelFont()
        val fm = g2.fontMetrics
        val y = (height - fm.height) / 2 + fm.ascent
        g2.drawString(text, 2, y)
        g2.dispose()
    }
}

/**
 * Flat, dark-themed text input for AWT container layouts.
 */
class AwtTextField(initialText: String = "", columns: Int = 20) : JTextField(initialText, columns) {
    init {
        background = AwtTheme.surfaceVariant
        foreground = AwtTheme.textPrimary
        caretColor = AwtTheme.primary
        selectionColor = AwtTheme.primary
        selectedTextColor = Color.WHITE
        font = AwtTheme.bodyFont
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AwtTheme.border, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        )
    }
}

/**
 * Flat, dark-themed code / log text area for AWT container layouts.
 */
class AwtTextArea(initialText: String = "", rows: Int = 8, columns: Int = 80) : JTextArea(initialText, rows, columns) {
    init {
        background = AwtTheme.surface
        foreground = AwtTheme.textPrimary
        caretColor = AwtTheme.primary
        selectionColor = AwtTheme.primary
        selectedTextColor = Color.WHITE
        font = AwtTheme.codeFont
        border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
    }
}
