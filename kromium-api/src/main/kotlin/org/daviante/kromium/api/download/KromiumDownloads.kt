package org.daviante.kromium.api.download

/**
 * Facade for download management operations.
 */
interface KromiumDownloads {
    /** Starts a download of the given URL. */
    fun startDownload(url: String)

    /** Cancels an active download by its ID. */
    fun cancelDownload(downloadId: Int): Boolean

    /** Pauses an active download by its ID. */
    fun pauseDownload(downloadId: Int): Boolean

    /** Resumes a paused download by its ID. */
    fun resumeDownload(downloadId: Int): Boolean

    /** Checks if a download is currently paused. */
    fun isDownloadPaused(downloadId: Int): Boolean
}
