package org.daviante.kromium.api.error

import org.daviante.kromium.api.ui.KromiumUiFramework
/**
 * Sealed exception hierarchy for all Kromium-specific errors.
 * 
 * Provides an [errorCode] and [isRecoverable] flag for robust error handling.
 */
sealed class KromiumException(
    @JvmField val errorCode: KromiumErrorCode,
    @JvmField val isRecoverable: Boolean,
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {

    class NotInitialized : KromiumException(
        KromiumErrorCode.INITIALIZATION_ERROR, 
        false, 
        "Kromium is not initialized. Call Kromium.initialize() first."
    )

    class Disposed : KromiumException(
        KromiumErrorCode.INITIALIZATION_ERROR, 
        false, 
        "Kromium has been disposed and cannot accept new operations."
    )

    class UnsupportedPlatform(
        @JvmField val os: String?,
        @JvmField val arch: String?
    ) : KromiumException(
        KromiumErrorCode.UNSUPPORTED_PLATFORM, 
        false, 
        "Unsupported platform: os=$os, arch=$arch"
    )

    class InstallationFailed(
        @JvmField val directory: String,
        cause: Throwable? = null
    ) : KromiumException(
        KromiumErrorCode.INSTALLATION_FAILED, 
        true, 
        "Failed to prepare installation directory: $directory", 
        cause
    )

    class AutoDownloadDisabled(
        @JvmField val directory: String
    ) : KromiumException(
        KromiumErrorCode.DOWNLOAD_DISABLED, 
        false, 
        "Auto-download is disabled (autoDownload = false), and no valid JCEF engine binaries were found at: $directory"
    )

    class NoBundleAvailable(
        @JvmField val platform: String,
        @JvmField val releaseTag: String?
    ) : KromiumException(
        KromiumErrorCode.NO_BUNDLE_AVAILABLE, 
        false, 
        "No compatible JCEF bundle found for $platform in release ${releaseTag ?: "latest"}"
    )

    class DownloadFailed(
        @JvmField val url: String,
        cause: Throwable? = null
    ) : KromiumException(
        KromiumErrorCode.DOWNLOAD_FAILED, 
        true, 
        "Failed to download engine bundle from: $url", 
        cause
    )

    class ChecksumMismatch(
        @JvmField val expected: String,
        @JvmField val actual: String
    ) : KromiumException(
        KromiumErrorCode.CHECKSUM_MISMATCH, 
        true, // Recoverable by retrying the download
        "Archive checksum mismatch — expected: $expected, actual: $actual. The download may be corrupted or tampered with."
    )

    class ExtractionFailed(
        @JvmField val archivePath: String,
        cause: Throwable? = null
    ) : KromiumException(
        KromiumErrorCode.EXTRACTION_FAILED, 
        true, 
        "Failed to extract engine bundle from: $archivePath", 
        cause
    )

    class MaliciousArchiveEntry(
        @JvmField val entryName: String
    ) : KromiumException(
        KromiumErrorCode.MALICIOUS_ARCHIVE, 
        false, 
        "Malicious archive entry detected (directory traversal): $entryName"
    )

    class BootstrapFailed(
        cause: Throwable? = null
    ) : KromiumException(
        KromiumErrorCode.BOOTSTRAP_FAILED, 
        false, 
        "CEF native bootstrap failed: ${cause?.message ?: "unknown error"}", 
        cause
    )

    class InstallationCorrupted(
        @JvmField val directory: String
    ) : KromiumException(
        KromiumErrorCode.INSTALLATION_CORRUPTED, 
        true, 
        "JCEF installation at $directory appears corrupted — re-download required."
    )

    class JsEvaluationTimeout(
        @JvmField val timeoutMs: Long
    ) : KromiumException(
        KromiumErrorCode.JS_TIMEOUT, 
        true, 
        "JavaScript evaluation timed out after ${timeoutMs}ms"
    )

    class InvalidConfig(
        @JvmField val detail: String
    ) : KromiumException(
        KromiumErrorCode.INVALID_CONFIG, 
        false, 
        "Invalid Kromium configuration: $detail"
    )

    class ProxyError(
        @JvmField val detail: String
    ) : KromiumException(
        KromiumErrorCode.PROXY_ERROR, 
        true, 
        "Proxy error: $detail"
    )

    class PdfPrintFailed(
        @JvmField val path: String,
        cause: Throwable? = null
    ) : KromiumException(
        KromiumErrorCode.PDF_PRINT_FAILED, 
        true, 
        "Failed to print web page to PDF: $path", 
        cause
    )

    class FrameworkRequired @JvmOverloads constructor(
        message: String = "UI framework is mandatory for OSR browser creation. No default OSR framework is permitted."
    ) : KromiumException(
        KromiumErrorCode.FRAMEWORK_REQUIRED,
        false,
        message
    )

    class CanvasNotAvailable @JvmOverloads constructor(
        @JvmField val framework: KromiumUiFramework,
        message: String = "No OSR canvas factory registered for framework: $framework. Please ensure kromium-${framework.name.lowercase()} is on the classpath."
    ) : KromiumException(
        KromiumErrorCode.CANVAS_NOT_AVAILABLE,
        false,
        message
    )
}
