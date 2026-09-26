package org.daviante.kromium.api.download

/**
 * Detailed download progress state.
 */
data class KromiumDownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long?,
    val fraction: Float // 0.0f .. 1.0f
) {
    val percentage: Int get() = (fraction * 100f).toInt().coerceIn(0, 100)

    companion object {
        val Initial = KromiumDownloadProgress(0L, null, 0.0f)
    }
}
