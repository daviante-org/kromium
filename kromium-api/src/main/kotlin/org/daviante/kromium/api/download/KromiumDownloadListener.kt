package org.daviante.kromium.api.download

/**
 * Callback for listening to and customizing file downloads.
 */
fun interface KromiumDownloadListener {
    fun onDownloadUpdated(item: KromiumDownloadItem)
}
