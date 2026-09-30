package org.daviante.kromium.api.download

/**
 * Metadata representing a file download event from the browser engine.
 */
data class KromiumDownloadItem(
    val id: Int,
    val url: String,
    val suggestedFileName: String,
    val totalBytes: Long,
    val receivedBytes: Long,
    val percentComplete: Int,
    val speed: Long,
    val isInProgress: Boolean,
    val isComplete: Boolean,
    val isCanceled: Boolean,
    val isPaused: Boolean = false,
    val fullPath: String = ""
)
