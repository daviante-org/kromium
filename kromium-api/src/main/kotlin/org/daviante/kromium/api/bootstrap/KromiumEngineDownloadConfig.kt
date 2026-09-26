package org.daviante.kromium.api.bootstrap

import java.io.File

/**
 * Configuration for downloading and installing the JCEF engine binaries.
 */
@ConsistentCopyVisibility
data class KromiumEngineDownloadConfig internal constructor(
    val installDir: File?,
    val source: KromiumEngineDownloadSource
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var installDir: File? = null
        private var source: KromiumEngineDownloadSource = KromiumEngineDownloadSource.GitHubRelease()

        fun installDir(installDir: File) = apply { this.installDir = installDir }
        fun source(source: KromiumEngineDownloadSource) = apply { this.source = source }

        fun build(): KromiumEngineDownloadConfig = KromiumEngineDownloadConfig(
            installDir, source
        )
    }
}

