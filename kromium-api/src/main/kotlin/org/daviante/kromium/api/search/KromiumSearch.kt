package org.daviante.kromium.api.search

/**
 * Facade for managing in-page text search operations within a browser instance.
 */
interface KromiumSearch {

    /**
     * Searches for [searchText] within the currently loaded web page.
     *
     * @param searchText The text string to search for.
     * @param forward Whether to search forward (`true`) or backward (`false`). Defaults to `true`.
     * @param matchCase Whether the search should be case-sensitive. Defaults to `false`.
     * @param findNext Whether this is a continuation of a previous search. Defaults to `false`.
     */
    fun find(
        searchText: String,
        forward: Boolean = true,
        matchCase: Boolean = false,
        findNext: Boolean = false
    )

    /**
     * Stops the current search operation and optionally clears search highlighting.
     *
     * @param clearSelection Whether to clear the active selection highlighting in the page. Defaults to `true`.
     */
    fun stopFinding(clearSelection: Boolean = true)
}
