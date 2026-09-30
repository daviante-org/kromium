package org.daviante.kromium.api.core

import org.daviante.kromium.api.download.KromiumDownloadProgress

/**
 * Observable lifecycle and operational states of the Kromium engine.
 */
sealed class KromiumState {
    data object Idle : KromiumState()
    data object Locating : KromiumState()
    data class Downloading(val progress: KromiumDownloadProgress) : KromiumState()
    data object Extracting : KromiumState()
    data object Initializing : KromiumState()
    data object Ready : KromiumState()
    data class Error(val cause: Throwable) : KromiumState()
    data object Disposed : KromiumState()
}
