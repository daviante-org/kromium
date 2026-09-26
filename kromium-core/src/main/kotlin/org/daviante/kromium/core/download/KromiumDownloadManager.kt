package org.daviante.kromium.core.download

import java.io.File

/**
 * Central coordinator for engine-agnostic download logic.
 * This class should be instantiated per browser client to manage its specific downloads.
 */
class KromiumDownloadManager {
    /** Manages the state of active and paused downloads. */
    val stateManager = KromiumDownloadStateManager()

    /**
     * Resolves the target download path, accounting for user interceptors and unique naming.
     * @return The absolute path to save the file, or null if the download was canceled.
     */
    fun <T> resolvePath(
        item: T,
        cleanName: String,
        defaultDir: File,
        interceptor: ((T, String) -> String?)?
    ): String? {
        return KromiumDownloadPathResolver.resolve(item, cleanName, defaultDir, interceptor)
    }
}
