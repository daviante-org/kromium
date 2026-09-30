package org.daviante.kromium.sample.swt.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

class SwtTheme(private val display: Display) {

    private val allocatedColors = mutableListOf<Color>()
    private val allocatedFonts = mutableListOf<Font>()

    fun color(r: Int, g: Int, b: Int): Color {
        val color = Color(display, RGB(r, g, b))
        allocatedColors.add(color)
        return color
    }

    val background = color(15, 23, 42)        // #0F172A
    val surface = color(30, 41, 59)           // #1E293B
    val surfaceVariant = color(51, 65, 85)    // #334155
    val border = color(71, 85, 105)           // #475569
    val primary = color(99, 102, 241)         // #6366F1
    val textPrimary = color(248, 250, 252)    // #F8FAFC
    val textSecondary = color(148, 163, 184)  // #94A3B8
    val success = color(16, 185, 129)         // #10B981
    val error = color(239, 68, 68)            // #EF4444

    fun font(name: String, height: Int, style: Int = SWT.NORMAL): Font {
        val font = Font(display, FontData(name, height, style))
        allocatedFonts.add(font)
        return font
    }

    val titleFont = font("Segoe UI", 11, SWT.BOLD)
    val bodyFont = font("Segoe UI", 10, SWT.NORMAL)
    val codeFont = font("Consolas", 10, SWT.NORMAL)
    val smallFont = font("Segoe UI", 9, SWT.NORMAL)

    fun registerDispose(shell: Shell) {
        shell.addDisposeListener {
            allocatedColors.forEach { if (!it.isDisposed) it.dispose() }
            allocatedFonts.forEach { if (!it.isDisposed) it.dispose() }
        }
    }
}
