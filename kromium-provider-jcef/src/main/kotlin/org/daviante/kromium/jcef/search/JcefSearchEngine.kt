package org.daviante.kromium.jcef.search

import org.daviante.kromium.api.search.KromiumSearch
import org.cef.browser.CefBrowser

/**
 * JCEF implementation of [KromiumSearch].
 */
internal class JcefSearchEngine(
    private val cefBrowser: CefBrowser
) : KromiumSearch {

    override fun find(
        searchText: String,
        forward: Boolean,
        matchCase: Boolean,
        findNext: Boolean
    ) {
        cefBrowser.find(searchText, forward, matchCase, findNext)
    }

    override fun stopFinding(clearSelection: Boolean) {
        cefBrowser.stopFinding(clearSelection)
    }
}
