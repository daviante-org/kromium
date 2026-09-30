package org.daviante.kromium.api.download

/**
 * Functional interface to dynamically resolve the target path before a download begins.
 */
fun interface KromiumBeforeDownloadListener {
    /**
     * Intercepts a download request before it starts.
     *
     * @param item The download item information.
     * @param suggestedFileName The file name suggested by the server.
     * @return The absolute path where the file should be saved, null to use the default path, or an empty string "" to cancel the download.
     */
    fun onBeforeDownload(item: KromiumDownloadItem, suggestedFileName: String): String?
}
