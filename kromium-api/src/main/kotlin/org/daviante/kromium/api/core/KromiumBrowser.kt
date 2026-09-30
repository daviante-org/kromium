package org.daviante.kromium.api.core

import org.daviante.kromium.api.automation.KromiumAutomation
import org.daviante.kromium.api.automation.KromiumClipboard
import org.daviante.kromium.api.devtools.KromiumDevTools
import org.daviante.kromium.api.download.KromiumDownloads
import org.daviante.kromium.api.automation.KromiumJsBridge
import org.daviante.kromium.api.navigation.KromiumNavigation
import org.daviante.kromium.api.storage.KromiumStorage
import org.daviante.kromium.api.ui.KromiumView
import org.daviante.kromium.api.proxy.KromiumProxyManager
import org.daviante.kromium.api.network.KromiumSecurity
import org.daviante.kromium.api.network.KromiumAssets
import org.daviante.kromium.api.search.KromiumSearch

/**
 * Main interface for the Kromium browser instance.
 * It is structured into multiple facades for separation of concerns.
 */
interface KromiumBrowser : AutoCloseable {
    val navigation: KromiumNavigation
    val view: KromiumView
    val search: KromiumSearch
    val devTools: KromiumDevTools
    val jsBridge: KromiumJsBridge
    val clipboard: KromiumClipboard
    val downloads: KromiumDownloads
    val automation: KromiumAutomation
    val storage: KromiumStorage
    val proxyManager: KromiumProxyManager
    val security: KromiumSecurity
    val assets: KromiumAssets

    /**
     * Closes this browser instance and cleans up underlying resources.
     */
    override fun close()
}
