package org.daviante.kromium.core.download

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for tracking active downloads and their pause states.
 */
class KromiumDownloadStateManager {
    private val activeCallbacks = ConcurrentHashMap<Int, KromiumEngineDownloadCallback>()
    private val pausedDownloads = ConcurrentHashMap<Int, Boolean>()

    fun register(id: Int, callback: KromiumEngineDownloadCallback) {
        activeCallbacks[id] = callback
    }

    fun unregister(id: Int) {
        activeCallbacks.remove(id)
        pausedDownloads.remove(id)
    }

    fun cancel(id: Int): Boolean {
        return activeCallbacks.remove(id)?.let {
            it.cancel()
            pausedDownloads.remove(id)
            true
        } ?: false
    }

    fun pause(id: Int): Boolean {
        return activeCallbacks[id]?.let {
            it.pause()
            pausedDownloads[id] = true
            true
        } ?: false
    }

    fun resume(id: Int): Boolean {
        return activeCallbacks[id]?.let {
            it.resume()
            pausedDownloads[id] = false
            true
        } ?: false
    }

    fun isPaused(id: Int): Boolean {
        return pausedDownloads[id] == true
    }
}
