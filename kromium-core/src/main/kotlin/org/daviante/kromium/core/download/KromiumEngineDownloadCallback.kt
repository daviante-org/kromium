package org.daviante.kromium.core.download

/**
 * Interface to abstract native provider download callbacks.
 */
interface KromiumEngineDownloadCallback {
    fun cancel()
    fun pause()
    fun resume()
}
