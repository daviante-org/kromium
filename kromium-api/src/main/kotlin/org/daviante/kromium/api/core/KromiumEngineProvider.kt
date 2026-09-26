package org.daviante.kromium.api.core

import org.daviante.kromium.api.config.KromiumConfig

/**
 * Service Provider Interface (SPI) for instantiating the Kromium engine.
 */
interface KromiumEngineProvider {
    /**
     * Creates and initializes a new KromiumEngine instance based on the provided configuration.
     * 
     * @param config The master configuration for the engine.
     * @return A fully initialized KromiumEngine instance.
     */
    fun createEngine(config: KromiumConfig): KromiumEngine
}
