package org.daviante.kromium.api.bootstrap

/**
 * Defines the strategy for acquiring the JCEF engine binaries.
 */
sealed interface KromiumEngineDownloadSource {

    /**
     * Do not attempt to download the engine. The application must manually place the
     * extracted JCEF binaries in the configured installation directory.
     */
    object Manual : KromiumEngineDownloadSource

    /**
     * Download the engine from a direct URL. The URL is resolved dynamically based on the current platform.
     */
    class DirectUrl(
        val bundleResolver: KromiumPlatformUrlResolver,
        val checksumResolver: KromiumPlatformUrlResolver? = null
    ) : KromiumEngineDownloadSource

    /**
     * Resolve and download the engine from a GitHub Release API endpoint.
     * The release assets must contain platform-specific tags (e.g., 'windows', 'x64', 'mac', 'aarch64').
     */
    data class GitHubRelease(
        val repoOwner: String = "JetBrains",
        val repoName: String = "JetBrainsRuntime",
        val releaseTag: String? = null,
        val apiBaseUrl: String = "https://api.github.com"
    ) : KromiumEngineDownloadSource

    companion object {
        @JvmStatic
        fun manual(): KromiumEngineDownloadSource = Manual

        /**
         * Uses a static, hardcoded URL regardless of the platform.
         */
        @JvmStatic
        @JvmOverloads
        fun directUrl(bundleUrl: String, checksumUrl: String? = null): KromiumEngineDownloadSource =
            DirectUrl(KromiumPlatformUrlResolver { bundleUrl }, checksumUrl?.let { KromiumPlatformUrlResolver { _ -> it } })

        /**
         * Uses a dynamic resolver function to determine the URL based on the OS and Architecture.
         */
        @JvmStatic
        @JvmOverloads
        fun directUrl(bundleResolver: KromiumPlatformUrlResolver, checksumResolver: KromiumPlatformUrlResolver? = null): KromiumEngineDownloadSource =
            DirectUrl(bundleResolver, checksumResolver)

        @JvmStatic
        @JvmOverloads
        fun githubRelease(
            repoOwner: String = "JetBrains",
            repoName: String = "JetBrainsRuntime",
            releaseTag: String? = null,
            apiBaseUrl: String = "https://api.github.com"
        ): KromiumEngineDownloadSource = GitHubRelease(repoOwner, repoName, releaseTag, apiBaseUrl)
    }
}

