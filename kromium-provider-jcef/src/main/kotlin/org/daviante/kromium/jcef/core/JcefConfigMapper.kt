package org.daviante.kromium.jcef.core

import org.daviante.kromium.api.config.KromiumBrowserConfig
import org.daviante.kromium.api.config.KromiumHardwareConfig
import org.daviante.kromium.api.config.KromiumConfig
import org.daviante.kromium.api.config.KromiumGpuMode
import org.daviante.kromium.api.config.KromiumLogSeverity
import org.daviante.kromium.api.config.KromiumLoggingConfig
import org.daviante.kromium.api.config.KromiumSecurityConfig
import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.core.platform.KromiumPlatformProvider
import org.daviante.kromium.jcef.bootstrap.JcefEngineRegistry
import org.cef.CefSettings
import java.io.File

object JcefConfigMapper {

    private val REGISTRY_SUPPRESSION_FLAGS: List<String> = listOf(
        "--no-default-browser-check",
        "--no-first-run",
        "--disable-breakpad",
        "--disable-crash-reporter",
        "--disable-metrics",
        "--disable-metrics-reporting",
        "--no-crash-upload",
        "--metrics-recording-only=0",
        "--disable-component-update",
        "--disable-background-networking",
        "--disable-domain-reliability",
        "--disable-sync",
        "--no-service-autorun",
        "--disable-background-mode",
        "--disable-logging",
        "--log-severity=fatal",
        "--log-level=3"
    )

    private const val REGISTRY_SUPPRESSION_FEATURES: String =
        "WinNativeNotification,CalculateNativeWinOcclusion,CertificateTransparencyComponentUpdater,MetricsReporting"

    fun toCefSettings(config: KromiumConfig): CefSettings {
        val settings = CefSettings()
        
        settings.windowless_rendering_enabled = config.browserConfig.isHeadless
        
        settings.log_severity = when (config.loggingConfig.logSeverity) {
            KromiumLogSeverity.VERBOSE -> CefSettings.LogSeverity.LOGSEVERITY_VERBOSE
            KromiumLogSeverity.INFO -> CefSettings.LogSeverity.LOGSEVERITY_INFO
            KromiumLogSeverity.WARNING -> CefSettings.LogSeverity.LOGSEVERITY_WARNING
            KromiumLogSeverity.ERROR -> CefSettings.LogSeverity.LOGSEVERITY_ERROR
            KromiumLogSeverity.FATAL -> CefSettings.LogSeverity.LOGSEVERITY_FATAL
            KromiumLogSeverity.DISABLE -> CefSettings.LogSeverity.LOGSEVERITY_DISABLE
            KromiumLogSeverity.DEFAULT -> {
                if (config.securityConfig.blockRegistryAndTelemetry) {
                    CefSettings.LogSeverity.LOGSEVERITY_FATAL
                } else {
                    CefSettings.LogSeverity.LOGSEVERITY_DEFAULT
                }
            }
        }
        
        val requestedCache = config.browserConfig.cachePath
        val effectiveCachePath = if (requestedCache != null) {
            if (requestedCache.isEmpty()) null else requestedCache
        } else {
            try {
                val base = (config.downloadConfig.installDir ?: JcefEngineRegistry.defaultInstallDir()).canonicalFile
                if (base == null || base.path.contains("..")) {
                    null
                } else {
                    val parent = base.parentFile ?: base
                    val target = File(parent, "cache").canonicalFile
                    if (!target.canonicalPath.startsWith(parent.canonicalPath)) {
                        null
                    } else {
                        target.apply { mkdirs() }.canonicalPath
                    }
                }
            } catch (_: Exception) { null }
        }

        effectiveCachePath?.let { path ->
            settings.cache_path = path
        }

        if (config.browserConfig.remoteDebuggingPort > 0) {
            settings.remote_debugging_port = config.browserConfig.remoteDebuggingPort
        }
        
        if (!config.securityConfig.sandboxEnabled) {
            settings.no_sandbox = true
        }

        return settings
    }

    fun buildCommandLineArgs(config: KromiumConfig, effectiveCachePath: String?): List<String> {
        val args = mutableListOf(
            "--disable-gpu-compositing",
            "--enable-begin-frame-scheduling",
            "--disable-extensions",
            "--disable-plugins",
            "--disable-site-isolation-trials"
        )

        // Cache path args
        effectiveCachePath?.let { path ->
            args.add("--root-cache-path=$path")
            args.add("--user-data-dir=$path")
        }

        // Sandbox
        if (!config.securityConfig.sandboxEnabled) {
            args.add("--no-sandbox")
        }

        // Security / Telemetry
        if (config.securityConfig.blockRegistryAndTelemetry) {
            args.addAll(REGISTRY_SUPPRESSION_FLAGS)
            args.add("--disable-features=$REGISTRY_SUPPRESSION_FEATURES")
        }

        // WebRTC
        if (config.securityConfig.webrtcIpHandlingPolicy != null) {
            args.add("--webrtc-ip-handling-policy=${config.securityConfig.webrtcIpHandlingPolicy}")
        } else if (config.securityConfig.blockRegistryAndTelemetry) {
            args.add("--webrtc-ip-handling-policy=default_public_interface_only")
        }

        // DNT
        if (config.securityConfig.doNotTrack) {
            args.add("--enable-do-not-track")
        }

        // SSO / Auth
        if (config.securityConfig.authServerAllowlist.isNotEmpty()) {
            args.add("--auth-server-allowlist=${config.securityConfig.authServerAllowlist.joinToString(",")}")
        }
        if (config.securityConfig.authNegotiateDelegateAllowlist.isNotEmpty()) {
            args.add("--auth-negotiate-delegate-allowlist=${config.securityConfig.authNegotiateDelegateAllowlist.joinToString(",")}")
        }

        // GPU Flags
        applyGpuFlags(config.hardwareConfig.gpuMode, args)

        // Startup Proxy Configuration
        config.browserConfig.proxy?.let { proxy ->
            args.addAll(proxy.toCommandLineArgs())
        }

        // Custom Args from Developer
        args.addAll(config.browserConfig.commandLineArgs)

        return args
    }

    private fun applyGpuFlags(gpuMode: KromiumGpuMode, args: MutableList<String>) {
        when (gpuMode) {
            KromiumGpuMode.HARDWARE -> {
                applyWindowsGpuShieldsIfNeeded(args, gpuMode)
            }
            KromiumGpuMode.COMPOSITING_DISABLED -> {
                args.add("--disable-gpu-compositing")
                applyWindowsGpuShieldsIfNeeded(args, gpuMode)
            }
            KromiumGpuMode.SOFTWARE -> {
                args.add("--disable-gpu")
                args.add("--use-gl=angle")
                args.add("--use-angle=swiftshader")
            }
            KromiumGpuMode.ANGLE_WARP -> {
                args.add("--use-gl=angle")
                args.add("--use-angle=warp")
                applyWindowsGpuShieldsIfNeeded(args, gpuMode)
            }
        }
    }

    private fun applyWindowsGpuShieldsIfNeeded(args: MutableList<String>, gpuMode: KromiumGpuMode) {
        val platform = try { KromiumPlatformProvider.instance.current() } catch (_: Throwable) { null }
        if (platform?.os?.isWindows == true) {
            args.add("--disable-direct-composition")
            args.add("--disable-direct-composition-video-overlays")
            if (args.none { it.equals("--disable-gpu-compositing", ignoreCase = true) }) {
                args.add("--disable-gpu-compositing")
            }
            args.add("--disable-gpu-watchdog")

            val skikoApi = System.getProperty("skiko.renderApi")
            val isSkikoOnDirect3D = skikoApi == null || skikoApi.equals("DIRECT3D", ignoreCase = true)
            if (isSkikoOnDirect3D && gpuMode != KromiumGpuMode.HARDWARE) {
                if (args.none { it.equals("--disable-gpu", ignoreCase = true) }) {
                    args.add("--disable-gpu")
                }
            }
        }
    }
}
