package org.daviante.kromium.api.ui

import java.util.function.Consumer

/**
 * Declarative builder for configuring browser right-click context menus.
 *
 * Supports Kotlin DSL blocks and fluent builder chaining, complete with built-in shortcuts
 * for common browser actions (Inspect Element, Copy Link, Web Search, Save Image).
 */
interface KromiumMenuBuilder {

    /**
     * Contextual parameters and action helpers for the current right-click event.
     */
    val context: KromiumContextMenuContext

    /**
     * Number of items currently present in the menu.
     */
    val count: Int

    /**
     * Clears all existing default menu items from the context menu.
     */
    fun clear(): KromiumMenuBuilder

    /**
     * Clears all existing default menu items (alias for Java / fluent style).
     */
    fun clearDefaults(): KromiumMenuBuilder = clear()

    /**
     * Removes a specific menu item by its integer command ID.
     */
    fun removeDefault(menuId: Int): KromiumMenuBuilder

    /**
     * Adds a standard clickable context menu item with an action callback.
     */
    fun addItem(
        label: String,
        enabled: Boolean = true,
        action: (KromiumContextMenuContext) -> Unit
    ): KromiumMenuBuilder

    /**
     * Kotlin DSL alias for [addItem].
     */
    fun item(
        label: String,
        enabled: Boolean = true,
        action: (KromiumContextMenuContext) -> Unit
    ): KromiumMenuBuilder = addItem(label, enabled, action)

    /**
     * Java Consumer overload for [addItem].
     */
    fun addItem(
        label: String,
        action: Consumer<KromiumContextMenuContext>
    ): KromiumMenuBuilder = addItem(label, true) { action.accept(it) }

    /**
     * Adds a checkable (checkbox) menu item.
     */
    fun addCheckItem(
        label: String,
        checked: Boolean,
        enabled: Boolean = true,
        onToggle: (Boolean) -> Unit
    ): KromiumMenuBuilder

    /**
     * Kotlin DSL alias for [addCheckItem].
     */
    fun checkItem(
        label: String,
        checked: Boolean,
        enabled: Boolean = true,
        onToggle: (Boolean) -> Unit
    ): KromiumMenuBuilder = addCheckItem(label, checked, enabled, onToggle)

    /**
     * Adds a radio menu item belonging to a mutual-exclusion group.
     */
    fun addRadioItem(
        label: String,
        checked: Boolean,
        groupId: Int,
        enabled: Boolean = true,
        onSelect: () -> Unit
    ): KromiumMenuBuilder

    /**
     * Kotlin DSL alias for [addRadioItem].
     */
    fun radioItem(
        label: String,
        checked: Boolean,
        groupId: Int,
        enabled: Boolean = true,
        onSelect: () -> Unit
    ): KromiumMenuBuilder = addRadioItem(label, checked, groupId, enabled, onSelect)

    /**
     * Adds a visual separator line between menu items.
     */
    fun addSeparator(): KromiumMenuBuilder

    /**
     * Kotlin DSL alias for [addSeparator].
     */
    fun separator(): KromiumMenuBuilder = addSeparator()

    /**
     * Adds a nested submenu with its own child builder.
     */
    fun addSubMenu(label: String, block: (KromiumMenuBuilder) -> Unit): KromiumMenuBuilder

    /**
     * Kotlin DSL alias for [addSubMenu].
     */
    fun subMenu(label: String, block: KromiumMenuBuilder.() -> Unit): KromiumMenuBuilder =
        addSubMenu(label) { builder -> builder.block() }

    // ==========================================
    // Native Browser Action Commands
    // ==========================================

    /** Adds the native Back command. */
    fun back(label: String = "Back"): KromiumMenuBuilder

    /** Adds the native Forward command. */
    fun forward(label: String = "Forward"): KromiumMenuBuilder

    /** Adds the native Reload command. */
    fun reload(label: String = "Reload"): KromiumMenuBuilder

    /** Adds the native Print command. */
    fun print(label: String = "Print..."): KromiumMenuBuilder

    /** Adds the native View Page Source command. */
    fun viewSource(label: String = "View Page Source"): KromiumMenuBuilder

    /** Adds the native Copy command. */
    fun copy(label: String = "Copy"): KromiumMenuBuilder

    /** Adds the native Cut command. */
    fun cut(label: String = "Cut"): KromiumMenuBuilder

    /** Adds the native Paste command. */
    fun paste(label: String = "Paste"): KromiumMenuBuilder

    /** Adds the native Select All command. */
    fun selectAll(label: String = "Select All"): KromiumMenuBuilder

    // ==========================================
    // Turnkey Built-In Action Shortcuts
    // ==========================================

    /**
     * Turnkey action: Opens DevTools targeting the clicked element coordinates.
     */
    fun inspectElement(label: String = "Inspect Element"): KromiumMenuBuilder =
        addItem(label) { it.inspectElement() }

    /**
     * Turnkey action: Copies the target link URL to clipboard if clicked on a hyperlink.
     */
    fun copyLink(label: String = "Copy Link Address"): KromiumMenuBuilder = apply {
        if (context.params.isLink()) {
            val url = context.params.linkUrl ?: ""
            addItem(label) { it.copyToClipboard(url) }
        }
    }

    /**
     * Turnkey action: Triggers browser download for image/media under cursor if present.
     */
    fun saveImageAs(label: String = "Save Image As..."): KromiumMenuBuilder = apply {
        if (context.params.hasMedia()) {
            val src = context.params.sourceUrl ?: ""
            if (src.isNotBlank()) {
                addItem(label) { it.startDownload(src) }
            }
        }
    }

    /**
     * Turnkey action: Copies media/image source URL to clipboard if present.
     */
    fun copyImageUrl(label: String = "Copy Image Address"): KromiumMenuBuilder = apply {
        if (context.params.hasMedia()) {
            val src = context.params.sourceUrl ?: ""
            if (src.isNotBlank()) {
                addItem(label) { it.copyToClipboard(src) }
            }
        }
    }

    /**
     * Turnkey action: Searches the web for the currently selected text.
     */
    fun searchWeb(
        labelFormat: String = "Search Google for \"%s\"",
        engineUrl: String = "https://www.google.com/search?q=%s",
        openInSystemBrowser: Boolean = true
    ): KromiumMenuBuilder = apply {
        if (context.params.hasSelection()) {
            val sel = context.params.selectionText ?: ""
            val truncated = if (sel.length > 24) sel.take(21) + "..." else sel
            val label = labelFormat.replace("%s", truncated)
            addItem(label) { it.searchWeb(sel, engineUrl, openInSystemBrowser) }
        }
    }

    companion object {
        /** Reserved starting integer ID for user-defined menu commands. */
        const val USER_COMMAND_FIRST: Int = 26500
    }
}
