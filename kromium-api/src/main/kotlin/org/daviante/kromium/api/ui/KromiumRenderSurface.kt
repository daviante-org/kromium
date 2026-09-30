package org.daviante.kromium.api.ui

import kotlin.reflect.KClass

/**
 * Represents an opaque rendering surface provided by the native engine (e.g. an AWT Component, 
 * an Android View, or a native texture ID). 
 * 
 * This follows the Adapter pattern to keep the core API agnostic of UI toolkits.
 */
interface KromiumRenderSurface {

    /**
     * Attempts to unwrap this generic surface into a platform-specific UI component 
     * (e.g., java.awt.Component for Swing/Compose Desktop).
     *
     * @param clazz The Kotlin class type of the expected native component.
     * @return The underlying component if it matches the type, or null otherwise.
     */
    fun <T : Any> unwrap(clazz: KClass<T>): T?
}
