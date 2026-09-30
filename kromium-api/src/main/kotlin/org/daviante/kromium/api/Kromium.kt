package org.daviante.kromium.api

import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.core.KromiumEngineProvider

/**
 * The main entry point for the Kromium library.
 */
object Kromium {

    private var activeEngine: KromiumEngine? = null

    /**
     * Creates the Kromium engine using the provided configuration and provider.
     * The engine is NOT automatically initialized. You must call `engine.initialize()` 
     * on a background thread so you can observe `engine.state` from the UI.
     *
     * @param config The master configuration for the engine.
     * @param provider The concrete provider implementation (e.g., JcefProvider) that creates the engine.
     * @return An uninitialized KromiumEngine instance.
     */
    @JvmStatic
    @Synchronized
    fun createEngine(config: KromiumConfig, provider: KromiumEngineProvider): KromiumEngine {
        activeEngine?.let { return it }
        val engine = provider.createEngine(config)
        activeEngine = engine
        return engine
    }
}
