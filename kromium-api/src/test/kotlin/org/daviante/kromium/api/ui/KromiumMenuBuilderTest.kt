package org.daviante.kromium.api.ui

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.devtools.KromiumDevTools
import org.daviante.kromium.api.download.KromiumDownloads
import org.daviante.kromium.api.navigation.KromiumNavigation
import java.awt.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KromiumMenuBuilderTest {

    private class TestMenuBuilder(
        override val context: KromiumContextMenuContext
    ) : KromiumMenuBuilder {
        val items = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        override var count: Int = 0

        override fun clear(): KromiumMenuBuilder = apply {
            items.clear()
            actions.clear()
            count = 0
        }

        override fun removeDefault(menuId: Int): KromiumMenuBuilder = apply {
            items.remove("default_$menuId")
        }

        override fun addItem(
            label: String,
            enabled: Boolean,
            action: (KromiumContextMenuContext) -> Unit
        ): KromiumMenuBuilder = apply {
            items.add(label)
            actions.add { action(context) }
            count++
        }

        override fun addCheckItem(
            label: String,
            checked: Boolean,
            enabled: Boolean,
            onToggle: (Boolean) -> Unit
        ): KromiumMenuBuilder = apply {
            items.add("[check] $label: $checked")
            count++
        }

        override fun addRadioItem(
            label: String,
            checked: Boolean,
            groupId: Int,
            enabled: Boolean,
            onSelect: () -> Unit
        ): KromiumMenuBuilder = apply {
            items.add("[radio] $label: $checked (group $groupId)")
            count++
        }

        override fun addSeparator(): KromiumMenuBuilder = apply {
            items.add("---")
            count++
        }

        override fun addSubMenu(label: String, block: (KromiumMenuBuilder) -> Unit): KromiumMenuBuilder = apply {
            items.add("[submenu] $label")
            count++
            val sub = TestMenuBuilder(context)
            block(sub)
            items.addAll(sub.items.map { "  $it" })
        }

        override fun back(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun forward(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun reload(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun print(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun viewSource(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun copy(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun cut(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun paste(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
        override fun selectAll(label: String): KromiumMenuBuilder = apply { items.add(label); count++ }
    }

    @Test
    fun testContextMenuParamsPredicates() {
        val params = KromiumContextMenuParams(
            x = 100,
            y = 200,
            linkUrl = "https://example.com/click",
            sourceUrl = "https://example.com/image.png",
            hasImage = true,
            selectionText = "Highlighted phrase",
            isEditable = true,
            isSpellCheckEnabled = true
        )

        assertEquals(100, params.x)
        assertEquals(200, params.y)
        assertTrue(params.isLink())
        assertTrue(params.hasMedia())
        assertTrue(params.hasSelection())
        assertTrue(params.isEditable)
        assertTrue(params.isSpellCheckEnabled)

        val emptyParams = KromiumContextMenuParams(x = 0, y = 0)
        assertFalse(emptyParams.isLink())
        assertFalse(emptyParams.hasMedia())
        assertFalse(emptyParams.hasSelection())
        assertFalse(emptyParams.isEditable)
    }

    @Test
    fun testMenuBuilderDslAndTurnkeyShortcuts() {
        val params = KromiumContextMenuParams(
            x = 50,
            y = 75,
            linkUrl = "https://example.com",
            sourceUrl = "https://example.com/pic.png",
            hasImage = true,
            selectionText = "Search Query",
            isEditable = true
        )
        val context = KromiumContextMenuContext(browser = null, params = params)
        val builder = TestMenuBuilder(context)

        builder.clear()
        builder.copyLink()
        builder.saveImageAs()
        builder.copyImageUrl()
        builder.searchWeb()
        builder.separator()
        builder.inspectElement()

        assertTrue(builder.items.contains("Copy Link Address"))
        assertTrue(builder.items.contains("Save Image As..."))
        assertTrue(builder.items.contains("Copy Image Address"))
        assertTrue(builder.items.contains("Search Google for \"Search Query\""))
        assertTrue(builder.items.contains("---"))
        assertTrue(builder.items.contains("Inspect Element"))
    }

    @Test
    fun testContextMenuListenerPresets() {
        val params = KromiumContextMenuParams(
            x = 10,
            y = 20,
            linkUrl = "https://example.com/link",
            selectionText = "Selected",
            isEditable = true
        )
        val context = KromiumContextMenuContext(browser = null, params = params)

        // Disabled preset
        val disabledBuilder = TestMenuBuilder(context)
        disabledBuilder.addItem("Dummy") {}
        assertEquals(1, disabledBuilder.count)
        KromiumContextMenuListener.disabled().onBuildContextMenu(disabledBuilder, context)
        assertEquals(0, disabledBuilder.count)

        // DevTools only preset
        val devToolsBuilder = TestMenuBuilder(context)
        KromiumContextMenuListener.devToolsOnly().onBuildContextMenu(devToolsBuilder, context)
        assertEquals(listOf("Inspect Element"), devToolsBuilder.items)

        // Minimal editing preset
        val minimalBuilder = TestMenuBuilder(context)
        KromiumContextMenuListener.minimalEditing(includeInspectElement = true).onBuildContextMenu(minimalBuilder, context)
        assertTrue(minimalBuilder.items.contains("Copy Link Address"))
        assertTrue(minimalBuilder.items.contains("Copy"))
        assertTrue(minimalBuilder.items.contains("Cut"))
        assertTrue(minimalBuilder.items.contains("Paste"))
        assertTrue(minimalBuilder.items.contains("Select All"))
        assertTrue(minimalBuilder.items.contains("Inspect Element"))
    }
}
