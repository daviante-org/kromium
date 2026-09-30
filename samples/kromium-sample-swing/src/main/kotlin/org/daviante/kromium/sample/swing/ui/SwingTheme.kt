package org.daviante.kromium.sample.swing.ui

import java.awt.Color
import java.awt.Font
import java.util.concurrent.CopyOnWriteArrayList

object SwingTheme {

    private val darkBackground = Color(0x0A, 0x0A, 0x0A)
    private val darkSurface = Color(0x14, 0x14, 0x14)
    private val darkSurfaceVariant = Color(0x1F, 0x1F, 0x1F)
    private val darkBorder = Color(0x33, 0x33, 0x33)
    private val darkPrimary = Color(0xFF, 0xFF, 0xFF)
    private val darkSecondary = Color(0xCC, 0xCC, 0xCC)
    private val darkTextPrimary = Color(0xF0, 0xF0, 0xF0)
    private val darkTextSecondary = Color(0xAA, 0xAA, 0xAA)
    private val darkError = Color(0xE5, 0x39, 0x35)

    private val lightBackground = Color(0xFF, 0xFF, 0xFF)
    private val lightSurface = Color(0xF8, 0xF8, 0xF8)
    private val lightSurfaceVariant = Color(0xEF, 0xEF, 0xEF)
    private val lightBorder = Color(0xCC, 0xCC, 0xCC)
    private val lightPrimary = Color(0x00, 0x00, 0x00)
    private val lightSecondary = Color(0x33, 0x33, 0x33)
    private val lightTextPrimary = Color(0x11, 0x11, 0x11)
    private val lightTextSecondary = Color(0x55, 0x55, 0x55)
    private val lightError = Color(0xD3, 0x2F, 0x2F)

    var isDark: Boolean = true
        private set

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun toggleTheme() {
        setTheme(!isDark)
    }

    fun setTheme(dark: Boolean) {
        if (isDark != dark) {
            isDark = dark
            listeners.forEach { it() }
        }
    }

    fun addThemeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeThemeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    val background: Color
        get() = if (isDark) darkBackground else lightBackground

    val surface: Color
        get() = if (isDark) darkSurface else lightSurface

    val surfaceVariant: Color
        get() = if (isDark) darkSurfaceVariant else lightSurfaceVariant

    val border: Color
        get() = if (isDark) darkBorder else lightBorder

    val primary: Color
        get() = if (isDark) darkPrimary else lightPrimary

    val onPrimary: Color
        get() = if (isDark) Color.BLACK else Color.WHITE

    val secondary: Color
        get() = if (isDark) darkSecondary else lightSecondary

    val textPrimary: Color
        get() = if (isDark) darkTextPrimary else lightTextPrimary

    val textSecondary: Color
        get() = if (isDark) darkTextSecondary else lightTextSecondary

    val error: Color
        get() = if (isDark) darkError else lightError

    fun fontMonospace(size: Float, bold: Boolean = false): Font {
        val style = if (bold) Font.BOLD else Font.PLAIN
        return Font(Font.MONOSPACED, style, size.toInt())
    }

    fun fontSans(size: Float, bold: Boolean = false): Font {
        val style = if (bold) Font.BOLD else Font.PLAIN
        return Font(Font.SANS_SERIF, style, size.toInt())
    }
}
