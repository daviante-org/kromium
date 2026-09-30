package org.daviante.kromium.api.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.scheme.KromiumAssetHandler
import org.daviante.kromium.api.cookie.KromiumCookieManager
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CompletableFuture

/**
 * The core interface representing the running Kromium engine.
 */
interface KromiumEngine {

    /**
     * The current state of the engine (e.g. downloading, extracting, ready).
     */
    val state: StateFlow<KromiumState>

    /**
     * Gets information about the current engine installation.
     */
    val info: KromiumEngineInfo

    /**
     * Initializes the engine asynchronously, downloading/extracting binaries if necessary.
     */
    suspend fun initialize()

    /**
     * Asynchronously initializes the engine returning a Java CompletableFuture.
     */
    fun initializeAsync(): CompletableFuture<Void?>

    /**
     * Disposes the engine, cleaning up native resources.
     */
    fun dispose()

    /**
     * Creates a new isolated browser client session.
     * @param config The client configuration containing listeners and session-specific options.
     */
    fun createClient(config: KromiumClientConfig = KromiumClientConfig.default()): KromiumClient

    /**
     * Global cookie manager for this engine.
     */
    val cookieManager: KromiumCookieManager

    /**
     * Registers a custom protocol scheme handler factory with the engine.
     *
     * @param schemeName The protocol scheme (e.g. "app").
     * @param domainName Optional domain name (e.g. "myapp" or null for all domains).
     * @param handler The asset handler fulfilling virtual requests.
     * @return True if registered successfully.
     */
    fun registerSchemeHandler(
        schemeName: String,
        domainName: String? = null,
        handler: KromiumAssetHandler
    ): Boolean

    /**
     * Programmatically cancels an in-progress download identified by its download ID across any active browser session.
     */
    fun cancelDownload(downloadId: Int): Boolean

    /**
     * Programmatically pauses an in-progress download identified by its download ID across any active browser session.
     */
    fun pauseDownload(downloadId: Int): Boolean

    /**
     * Programmatically resumes a paused download identified by its download ID across any active browser session.
     */
    fun resumeDownload(downloadId: Int): Boolean

    /**
     * Checks whether an in-progress download is currently paused across any active browser session.
     */
    fun isDownloadPaused(downloadId: Int): Boolean

    /**
     * Clears all registered custom scheme handler factories.
     *
     * @return True if cleared successfully.
     */
    fun clearSchemeHandlers(): Boolean
}
