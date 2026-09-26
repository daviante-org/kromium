package org.daviante.kromium.jcef.download
import org.daviante.kromium.jcef.core.JcefKromiumClient

import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.core.download.KromiumEngineDownloadCallback
import org.daviante.kromium.core.download.KromiumDownloadManager
import org.cef.browser.CefBrowser
import org.cef.callback.CefBeforeDownloadCallback
import org.cef.callback.CefDownloadItem
import org.cef.callback.CefDownloadItemCallback
import org.cef.handler.CefDownloadHandlerAdapter

/**
 * Bridges JCEF's native CefDownloadHandler to Kromium's generic download management.
 */
internal class JcefDownloadAdapter(
    private val client: JcefKromiumClient,
    private val manager: KromiumDownloadManager
) : CefDownloadHandlerAdapter() {

    override fun onBeforeDownload(
        browser: CefBrowser?,
        downloadItem: CefDownloadItem?,
        suggestedName: String?,
        callback: CefBeforeDownloadCallback?
    ): Boolean {
        if (callback == null || downloadItem == null) return false

        val rawName = suggestedName?.takeIf { it.isNotBlank() }
            ?: downloadItem.suggestedFileName?.takeIf { it.isNotBlank() }
            ?: "download"
        val cleanName = rawName.substringAfterLast('/').substringAfterLast('\\')
            .replace("[?%*:|\"<>]".toRegex(), "_")
            .ifBlank { "download" }

        val apiItem = downloadItem.toApiItem(cleanName, "", false)

        val finalPath = manager.resolvePath<KromiumDownloadItem>(
            item = apiItem,
            cleanName = cleanName,
            defaultDir = client.downloadDirectory,
            interceptor = client.onBeforeDownloadListener?.let { listener ->
                { item, name -> listener.onBeforeDownload(item, name) }
            }
        )

        return if (finalPath != null) {
            callback.Continue(finalPath, false)
            true
        } else {
            callback.Continue("", false)
            false
        }
    }

    override fun onDownloadUpdated(
        browser: CefBrowser?,
        downloadItem: CefDownloadItem?,
        callback: CefDownloadItemCallback?
    ) {
        if (downloadItem == null) return

        if (callback != null) {
            if (downloadItem.isComplete || downloadItem.isCanceled) {
                manager.stateManager.unregister(downloadItem.id)
            } else {
                manager.stateManager.register(downloadItem.id, object : KromiumEngineDownloadCallback {
                    override fun cancel() { callback.cancel() }
                    override fun pause() { callback.pause() }
                    override fun resume() { callback.resume() }
                })
            }
        }

        val fullPath = downloadItem.fullPath ?: ""
        val isPaused = manager.stateManager.isPaused(downloadItem.id)
        val apiItem = downloadItem.toApiItem(downloadItem.suggestedFileName ?: "download", fullPath, isPaused)

        client.downloadListener?.onDownloadUpdated(apiItem)
    }

    private fun CefDownloadItem.toApiItem(suggestedName: String, resolvedPath: String, isPaused: Boolean): KromiumDownloadItem {
        return KromiumDownloadItem(
            id = this.id,
            url = this.url ?: "",
            suggestedFileName = suggestedName,
            fullPath = resolvedPath,
            totalBytes = this.totalBytes,
            receivedBytes = this.receivedBytes,
            percentComplete = this.percentComplete,
            speed = this.currentSpeed,
            isInProgress = this.isInProgress,
            isComplete = this.isComplete,
            isCanceled = this.isCanceled,
            isPaused = isPaused
        )
    }
}
