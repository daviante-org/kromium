package org.daviante.kromium.jcef.ui

import org.daviante.kromium.api.ui.KromiumContextMenuContext
import org.daviante.kromium.api.ui.KromiumMenuBuilder
import org.cef.callback.CefMenuModel
import java.util.concurrent.atomic.AtomicInteger

/**
 * JCEF implementation of [KromiumMenuBuilder] wrapping underlying [CefMenuModel].
 */
internal class JcefMenuBuilder internal constructor(
    private val model: CefMenuModel,
    private val nextCommandId: () -> Int,
    private val actionMap: MutableMap<Int, (KromiumContextMenuContext) -> Unit>,
    override val context: KromiumContextMenuContext
) : KromiumMenuBuilder {

    constructor(
        model: CefMenuModel,
        context: KromiumContextMenuContext,
        actionMap: MutableMap<Int, (KromiumContextMenuContext) -> Unit>
    ) : this(
        model = model,
        nextCommandId = createIdGenerator(),
        actionMap = actionMap,
        context = context
    )

    override val count: Int
        get() = model.count

    override fun clear(): KromiumMenuBuilder = apply {
        model.clear()
    }

    override fun removeDefault(menuId: Int): KromiumMenuBuilder = apply {
        model.remove(menuId)
    }

    override fun addItem(
        label: String,
        enabled: Boolean,
        action: (KromiumContextMenuContext) -> Unit
    ): KromiumMenuBuilder = apply {
        val cmdId = nextCommandId()
        actionMap[cmdId] = action
        model.addItem(cmdId, label)
        model.setEnabled(cmdId, enabled)
    }

    override fun addCheckItem(
        label: String,
        checked: Boolean,
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ): KromiumMenuBuilder = apply {
        val cmdId = nextCommandId()
        actionMap[cmdId] = {
            onToggle(!checked)
        }
        model.addCheckItem(cmdId, label)
        model.setChecked(cmdId, checked)
        model.setEnabled(cmdId, enabled)
    }

    override fun addRadioItem(
        label: String,
        checked: Boolean,
        groupId: Int,
        enabled: Boolean,
        onSelect: () -> Unit
    ): KromiumMenuBuilder = apply {
        val cmdId = nextCommandId()
        actionMap[cmdId] = { onSelect() }
        model.addRadioItem(cmdId, label, groupId)
        model.setChecked(cmdId, checked)
        model.setEnabled(cmdId, enabled)
    }

    override fun addSeparator(): KromiumMenuBuilder = apply {
        model.addSeparator()
    }

    override fun addSubMenu(label: String, block: (KromiumMenuBuilder) -> Unit): KromiumMenuBuilder = apply {
        val cmdId = nextCommandId()
        val subModel = model.addSubMenu(cmdId, label)
        val subBuilder = JcefMenuBuilder(
            model = subModel,
            nextCommandId = nextCommandId,
            actionMap = actionMap,
            context = context
        )
        block(subBuilder)
    }

    override fun back(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_BACK, label)
    }

    override fun forward(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_FORWARD, label)
    }

    override fun reload(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_RELOAD, label)
    }

    override fun print(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_PRINT, label)
    }

    override fun viewSource(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_VIEW_SOURCE, label)
    }

    override fun copy(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_COPY, label)
    }

    override fun cut(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_CUT, label)
    }

    override fun paste(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_PASTE, label)
    }

    override fun selectAll(label: String): KromiumMenuBuilder = apply {
        model.addItem(CefMenuModel.MenuId.MENU_ID_SELECT_ALL, label)
    }

    companion object {
        private fun createIdGenerator(): () -> Int {
            val counter = AtomicInteger(KromiumMenuBuilder.USER_COMMAND_FIRST)
            return { counter.getAndIncrement() }
        }
    }
}
