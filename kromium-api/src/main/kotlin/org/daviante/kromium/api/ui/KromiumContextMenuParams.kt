package org.daviante.kromium.api.ui

/**
 * Encapsulates the contextual parameters of the user's right-click or context menu gesture.
 *
 * Provides provider-agnostic access to clicked coordinates, hyperlinks, media URLs,
 * selected text, and input field state.
 *
 * @property x Horizontal coordinate relative to the browser viewport where the right-click occurred.
 * @property y Vertical coordinate relative to the browser viewport where the right-click occurred.
 * @property linkUrl The target URL if the right-click occurred on a hyperlink, or null otherwise.
 * @property unfilteredLinkUrl The unfiltered target URL if on a hyperlink, or null otherwise.
 * @property sourceUrl The source URL if right-click occurred on media (image, audio, video), or null.
 * @property hasImage True if the clicked element has image contents.
 * @property pageUrl URL of the top-level page where the context menu was triggered.
 * @property frameUrl URL of the specific subframe where the context menu was triggered.
 * @property selectionText Any text currently highlighted/selected by the user, or null.
 * @property misspelledWord The misspelled word under cursor if spellcheck is active, or null.
 * @property isEditable True if the context menu was triggered inside an editable field (input, textarea).
 * @property isSpellCheckEnabled True if spell checking is enabled for the clicked context.
 */
data class KromiumContextMenuParams(
    val x: Int,
    val y: Int,
    val linkUrl: String? = null,
    val unfilteredLinkUrl: String? = null,
    val sourceUrl: String? = null,
    val hasImage: Boolean = false,
    val pageUrl: String? = null,
    val frameUrl: String? = null,
    val selectionText: String? = null,
    val misspelledWord: String? = null,
    val isEditable: Boolean = false,
    val isSpellCheckEnabled: Boolean = false
) {
    /** True if text is currently highlighted or selected. */
    fun hasSelection(): Boolean = !selectionText.isNullOrBlank()

    /** True if the click was on a hyperlink. */
    fun isLink(): Boolean = !linkUrl.isNullOrBlank()

    /** True if the click was on media or an image element. */
    fun hasMedia(): Boolean = hasImage || !sourceUrl.isNullOrBlank()
}
