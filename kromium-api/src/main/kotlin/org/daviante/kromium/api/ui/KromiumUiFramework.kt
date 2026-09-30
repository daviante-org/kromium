package org.daviante.kromium.api.ui

import org.daviante.kromium.api.error.KromiumException

/**
 * UI framework targets supported by Kromium.
 *
 * Configured via [org.daviante.kromium.api.config.KromiumClientConfig.framework] so the underlying engine
 * uses the dedicated, native OSR frame/surface optimized for the chosen UI toolkit.
 */
enum class KromiumUiFramework {
    /**
     * Standard Java Swing toolkit. Hosts OSR output in a native [javax.swing.JPanel].
     */
    SWING,

    /**
     * Pure Java AWT toolkit. Hosts OSR output in a native [java.awt.Canvas].
     */
    AWT,

    /**
     * Eclipse SWT toolkit. Hosts OSR output in a native [org.eclipse.swt.widgets.Canvas].
     */
    SWT,

    /**
     * JavaFX toolkit. Hosts OSR output in a native JavaFX Canvas/Region.
     */
    JAVAFX,

    /**
     * Jetpack Compose Desktop toolkit.
     */
    COMPOSE,

    /**
     * Off-screen headless rendering without a UI window hierarchy.
     */
    HEADLESS;

    companion object {
        /**
         * Resolves a framework enum by name (case-insensitive).
         * Throws [KromiumException.FrameworkRequired] if name is null or blank,
         * or [KromiumException.InvalidConfig] if unknown.
         */
        @JvmStatic
        fun fromString(name: String?): KromiumUiFramework {
            if (name.isNullOrBlank()) {
                throw KromiumException.FrameworkRequired("UI framework name cannot be null or blank.")
            }
            val trimmed = name.trim()
            return values().firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                ?: throw KromiumException.InvalidConfig(
                    "Unknown UI framework '$trimmed'. Supported frameworks are: ${values().joinToString { it.name }}."
                )
        }
    }
}
