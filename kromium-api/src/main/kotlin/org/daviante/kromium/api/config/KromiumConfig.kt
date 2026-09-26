package org.daviante.kromium.api.config

import org.daviante.kromium.api.bootstrap.KromiumEngineDownloadConfig
import org.daviante.kromium.api.error.KromiumException
import java.util.logging.Logger

/**
 * The master configuration object for the Kromium engine.
 *
 * This configuration is composed of specialized sub-configurations.
 * Use the fluent builder to configure specific domains:
 * ```kotlin
 * val config = KromiumConfig.builder()
 *     .downloadConfig { it.autoDownload(true) }
 *     .hardwareConfig { it.gpuMode(KromiumGpuMode.HARDWARE) }
 *     .build()
 * ```
 */
@ConsistentCopyVisibility
data class KromiumConfig internal constructor(
    val downloadConfig: KromiumEngineDownloadConfig,
    val browserConfig: KromiumBrowserConfig,
    val hardwareConfig: KromiumHardwareConfig,
    val securityConfig: KromiumSecurityConfig,
    val loggingConfig: KromiumLoggingConfig
) {
    /**
     * Validates this configuration and throws [KromiumException.InvalidConfig]
     * if any values are invalid.
     */
    fun validate() {
        if (browserConfig.remoteDebuggingPort < 0 || browserConfig.remoteDebuggingPort > 65535) {
            throw KromiumException.InvalidConfig(
                "remoteDebuggingPort must be 0-65535, got: ${browserConfig.remoteDebuggingPort}"
            )
        }

        browserConfig.cachePath?.let { path ->
            if (path.contains("..")) {
                throw KromiumException.InvalidConfig("cachePath contains path traversal sequence: $path")
            }
        }

        val logger = Logger.getLogger("KromiumConfig")

        val dangerousFlags = browserConfig.commandLineArgs.filter { arg ->
            val lower = arg.lowercase()
            lower.contains("--no-sandbox") ||
                lower.contains("--disable-web-security") ||
                lower.contains("--allow-running-insecure-content")
        }

        for (flag in dangerousFlags) {
            logger.warning("⚠️ Dangerous command-line flag detected: $flag — This significantly reduces security.")
        }

        if (!securityConfig.sandboxEnabled) {
            logger.warning("⚠️ Sandbox is disabled. Do not load untrusted web content without sandboxing.")
        }
    }

    companion object {
        /** Returns a new builder for [KromiumConfig] initialized with default values. */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var downloadConfigBuilder = KromiumEngineDownloadConfig.builder()
        private var browserConfigBuilder = KromiumBrowserConfig.builder()
        private var hardwareConfigBuilder = KromiumHardwareConfig.builder()
        private var securityConfigBuilder = KromiumSecurityConfig.builder()
        private var loggingConfigBuilder = KromiumLoggingConfig.builder()

        /**
         * Configures engine downloading and installation paths.
         */
        fun downloadConfig(block: (KromiumEngineDownloadConfig.Builder) -> Unit) = apply {
            block(downloadConfigBuilder)
        }

        /**
         * Configures general browser behavior, cache, and user agent.
         */
        fun browserConfig(block: (KromiumBrowserConfig.Builder) -> Unit) = apply {
            block(browserConfigBuilder)
        }

        /**
         * Configures interaction with the host OS and GPU (e.g. Process Models, GPU acceleration).
         */
        fun hardwareConfig(block: (KromiumHardwareConfig.Builder) -> Unit) = apply {
            block(hardwareConfigBuilder)
        }

        /**
         * Configures security boundaries and telemetry blockers.
         */
        fun securityConfig(block: (KromiumSecurityConfig.Builder) -> Unit) = apply {
            block(securityConfigBuilder)
        }

        /**
         * Configures engine-level logging severity.
         */
        fun loggingConfig(block: (KromiumLoggingConfig.Builder) -> Unit) = apply {
            block(loggingConfigBuilder)
        }

        // Direct setters for Java interoperability (since Java doesn't support the trailing lambda DSL elegantly)

        fun downloadConfig(config: KromiumEngineDownloadConfig) = apply {
            // Overwrite the builder state with the provided config by re-initializing the builder
            downloadConfigBuilder = KromiumEngineDownloadConfig.builder()
                .source(config.source)
            config.installDir?.let { downloadConfigBuilder.installDir(it) }
        }

        fun browserConfig(config: KromiumBrowserConfig) = apply {
            browserConfigBuilder = KromiumBrowserConfig.builder()
                .cachePath(config.cachePath)
                .isHeadless(config.isHeadless)
                .remoteDebuggingPort(config.remoteDebuggingPort)
                .proxy(config.proxy)
                .addArgs(*config.commandLineArgs.toTypedArray())
                .addCustomSchemes(*config.customSchemes.toTypedArray())
                .addSchemeRegistrations(*config.schemeRegistrations.toTypedArray())
        }

        fun hardwareConfig(config: KromiumHardwareConfig) = apply {
            hardwareConfigBuilder = KromiumHardwareConfig.builder()
                .processModel(config.processModel)
                .gpuMode(config.gpuMode)
        }

        fun securityConfig(config: KromiumSecurityConfig) = apply {
            securityConfigBuilder = KromiumSecurityConfig.builder()
                .sandboxEnabled(config.sandboxEnabled)
                .blockRegistryAndTelemetry(config.blockRegistryAndTelemetry)
                .doNotTrack(config.doNotTrack)
                .webrtcIpHandlingPolicy(config.webrtcIpHandlingPolicy)
                .authServerAllowlist(config.authServerAllowlist)
                .authNegotiateDelegateAllowlist(config.authNegotiateDelegateAllowlist)
        }

        fun loggingConfig(config: KromiumLoggingConfig) = apply {
            loggingConfigBuilder = KromiumLoggingConfig.builder()
                .logSeverity(config.logSeverity)
        }

        fun build(): KromiumConfig {
            return KromiumConfig(
                downloadConfig = downloadConfigBuilder.build(),
                browserConfig = browserConfigBuilder.build(),
                hardwareConfig = hardwareConfigBuilder.build(),
                securityConfig = securityConfigBuilder.build(),
                loggingConfig = loggingConfigBuilder.build()
            )
        }
    }
}

