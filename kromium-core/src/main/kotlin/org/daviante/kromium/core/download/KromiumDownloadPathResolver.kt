package org.daviante.kromium.core.download

import org.daviante.kromium.core.logging.KromiumLogger
import java.io.File

/**
 * Resolves the final download path, accounting for user interceptors and directory writability.
 */
object KromiumDownloadPathResolver {
    private const val TAG = "KromiumDownloadPathResolver"

    /**
     * Resolves the target download path.
     * @return The absolute path to save the file, or null if the download was canceled.
     */
    fun <T> resolve(
        item: T,
        cleanName: String,
        defaultDir: File,
        interceptor: ((T, String) -> String?)?
    ): String? {
        val targetDir = if (defaultDir.canWrite() || defaultDir.mkdirs()) {
            defaultDir
        } else {
            File(System.getProperty("user.home"), "Downloads")
        }

        val defaultTargetFile = KromiumUniqueFileNameGenerator.generate(targetDir, cleanName)
        val defaultTargetPath = defaultTargetFile.absolutePath

        val customPath = interceptor?.invoke(item, cleanName)

        return when {
            customPath != null && customPath.isBlank() -> {
                KromiumLogger.i(TAG, "Download canceled by interceptor: $cleanName")
                null
            }
            customPath != null -> {
                try {
                    File(customPath).canonicalFile.absolutePath
                } catch (t: Throwable) {
                    KromiumLogger.e(TAG, "Invalid custom download path: $customPath, falling back to default.", t)
                    defaultTargetPath
                }
            }
            else -> {
                defaultTargetPath
            }
        }
    }
}
