package org.daviante.kromium.jcef

import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.core.KromiumEngineProvider
import org.daviante.kromium.jcef.core.JcefKromiumEngine

/**
 * The entry point provider for the JCEF implementation of Kromium.
 */
object JcefProvider : KromiumEngineProvider {
    override fun createEngine(config: KromiumConfig): KromiumEngine {
        return JcefKromiumEngine(config)
    }
}
