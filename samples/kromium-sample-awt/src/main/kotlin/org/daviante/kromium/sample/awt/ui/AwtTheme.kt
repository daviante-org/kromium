package org.daviante.kromium.sample.awt.ui

import java.awt.Color
import java.awt.Font

object AwtTheme {
    val background = Color(15, 23, 42)        // #0F172A
    val surface = Color(30, 41, 59)           // #1E293B
    val surfaceVariant = Color(51, 65, 85)    // #334155
    val border = Color(71, 85, 105)           // #475569
    val primary = Color(99, 102, 241)         // #6366F1
    val textPrimary = Color(248, 250, 252)    // #F8FAFC
    val textSecondary = Color(148, 163, 184)  // #94A3B8
    val success = Color(16, 185, 129)         // #10B981
    val error = Color(239, 68, 68)            // #EF4444

    private val sansFontName = Font.SANS_SERIF
    private val monoFontName = Font.MONOSPACED

    val titleFont = Font(sansFontName, Font.BOLD, 13)
    val bodyFont = Font(sansFontName, Font.PLAIN, 12)
    val codeFont = Font(monoFontName, Font.PLAIN, 12)
    val smallFont = Font(sansFontName, Font.PLAIN, 11)
}
