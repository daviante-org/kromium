package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.cookie.KromiumCookieManager
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.core.KromiumEngine
import org.daviante.kromium.api.core.KromiumEngineInfo
import org.daviante.kromium.api.core.KromiumState
import org.daviante.kromium.api.download.KromiumDownloadProgress
import org.daviante.kromium.api.scheme.KromiumAssetHandler
import org.daviante.kromium.jcef.bootstrap.JcefBootstrapper
import org.daviante.kromium.jcef.bootstrap.JcefEngineDownloader
import org.daviante.kromium.jcef.bootstrap.JcefEngineExtractor
import org.daviante.kromium.jcef.bootstrap.JcefEngineRegistry
import org.daviante.kromium.jcef.bootstrap.JcefVersionConstants
import org.daviante.kromium.jcef.cookie.JcefCookieManager
import org.daviante.kromium.jcef.scheme.JcefSchemeHandlerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cef.CefApp
import org.daviante.kromium.core.util.KromiumFutureBridge
import java.util.concurrent.CompletableFuture
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

/**
 * JCEF implementation of the KromiumEngine.
 */
class JcefKromiumEngine internal constructor(
    private val config: KromiumConfig
) : KromiumEngine {

    private val _state = MutableStateFlow<KromiumState>(KromiumState.Idle)
    override val state: StateFlow<KromiumState> = _state.asStateFlow()

    private val mutex = Mutex()

    private var cefApp: CefApp? = null

    internal val activeClients = CopyOnWriteArraySet<JcefKromiumClient>()

    override val cookieManager: KromiumCookieManager by lazy {
        JcefCookieManager()
    }

    override val info: KromiumEngineInfo
        get() {
            val installDir = config.downloadConfig.installDir ?: JcefEngineRegistry.defaultInstallDir()

            return KromiumEngineInfo(
                installDir = installDir,
                isInstalled = JcefEngineRegistry.isInstalled(installDir),
                engineVersion = JcefVersionConstants.JCEF_VERSION,
                browserVersion = JcefVersionConstants.CHROMIUM_VERSION,
                providerMetadata = mapOf(
                    "cefVersion" to JcefVersionConstants.CEF_VERSION
                )
            )
        }

    override suspend fun initialize() {
        config.validate()

        if (_state.value is KromiumState.Ready) {
            return
        }

        mutex.withLock {
            if (_state.value is KromiumState.Ready) {
                return
            }
            
            _state.value = KromiumState.Initializing

            try {
                val installDir = config.downloadConfig.installDir ?: JcefEngineRegistry.defaultInstallDir()

                // 1. Download if not installed
                if (!JcefEngineRegistry.isInstalled(installDir)) {
                    _state.value = KromiumState.Downloading(KromiumDownloadProgress.Initial)
                    
                    val downloader = JcefEngineDownloader()
                    val resolvedPackage = downloader.resolvePackageUrl(config.downloadConfig)
                    val archiveFile = File.createTempFile("kromium-jcef-bundle", resolvedPackage.archiveExtension)
                    
                    try {
                        downloader.downloadToFile(resolvedPackage, archiveFile) { progress ->
                            _state.value = KromiumState.Downloading(progress)
                        }
                        
                        _state.value = KromiumState.Extracting
                        JcefEngineExtractor.extractArchive(archiveFile, installDir)
                        JcefEngineRegistry.markInstalled(installDir)
                    } finally {
                        archiveFile.delete()
                        downloader.close()
                    }
                }

                // 2. Bootstrap JCEF
                _state.value = KromiumState.Initializing
                val app = JcefBootstrapper.bootstrap(config, config.browserConfig.customSchemes)
                cefApp = app

                // Register pre-configured scheme handlers
                for (registration in config.browserConfig.schemeRegistrations) {
                    val factory = JcefSchemeHandlerFactory(registration.handler)
                    val domain = registration.domainName ?: ""
                    app.registerSchemeHandlerFactory(registration.schemeName, domain, factory)
                }
                
                _state.value = KromiumState.Ready
            } catch (e: Exception) {
                _state.value = KromiumState.Error(e)
                throw e
            }
        }
    }

    override fun initializeAsync(): CompletableFuture<Void?> {
        return KromiumFutureBridge.toCompletableFuture {
            initialize()
            null
        }
    }

    override fun dispose() {
        cefApp?.dispose()
        cefApp = null
        _state.value = KromiumState.Idle
    }

    override fun createClient(config: KromiumClientConfig): KromiumClient {
        val app = cefApp ?: throw IllegalStateException("Engine is not initialized")
        val cefClient = app.createClient()
        val client = JcefKromiumClient(this, cefClient, this.config.browserConfig.isHeadless, config)
        activeClients.add(client)
        return client
    }

    override fun cancelDownload(downloadId: Int): Boolean =
        activeClients.any { it.cancelDownload(downloadId) }

    override fun pauseDownload(downloadId: Int): Boolean =
        activeClients.any { it.pauseDownload(downloadId) }

    override fun resumeDownload(downloadId: Int): Boolean =
        activeClients.any { it.resumeDownload(downloadId) }

    override fun isDownloadPaused(downloadId: Int): Boolean =
        activeClients.any { it.isDownloadPaused(downloadId) }

    override fun registerSchemeHandler(
        schemeName: String,
        domainName: String?,
        handler: KromiumAssetHandler
    ): Boolean {
        val app = cefApp ?: throw IllegalStateException("Engine is not initialized")
        val factory = JcefSchemeHandlerFactory(handler)
        val domain = domainName ?: ""
        return app.registerSchemeHandlerFactory(schemeName, domain, factory)
    }

    override fun clearSchemeHandlers(): Boolean {
        val app = cefApp ?: return false
        return app.clearSchemeHandlerFactories()
    }
}
