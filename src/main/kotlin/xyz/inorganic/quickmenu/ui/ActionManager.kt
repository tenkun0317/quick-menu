package xyz.inorganic.quickmenu.ui

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.ui.components.ConfirmScreen
import java.util.Collections

class ActionManager(
    private val onStateChanged: () -> Unit
) {
    fun moveAction(data: ActionButtonData, direction: Int, isSearching: Boolean) {
        if (isSearching) return
        val actions = NavigationState.getCurrentChildren()
        val index = actions.indexOf(data)
        val newIndex = index + direction
        if (newIndex in 0 until actions.size) {
            Collections.swap(actions, index, newIndex)
            ActionButtonDataHandler.save()
            onStateChanged()
        }
    }

    fun deleteAction(data: ActionButtonData, isSearching: Boolean, onConfirm: () -> Unit) {
        val confirmScreen = ConfirmScreen({ confirmed ->
            if (confirmed) {
                performDelete(data, isSearching)
            }
            onConfirm()
        }, Component.translatable("menu.main.delete.confirm.title"), Component.translatable("menu.main.delete.confirm.message", data.name))
        Minecraft.getInstance().setScreen(confirmScreen)
    }

    private fun performDelete(data: ActionButtonData, isSearching: Boolean) {
        if (isSearching) {
            fun findAndDelete(list: MutableList<ActionButtonData>): Boolean {
                if (list.remove(data)) return true
                for (action in list) {
                    if (action.isFolder && findAndDelete(action.children)) return true
                }
                return false
            }
            findAndDelete(ActionButtonDataHandler.actions)
        } else {
            val actions = NavigationState.getCurrentChildren()
            actions.remove(data)
        }

        ActionButtonDataHandler.save()
        onStateChanged()
    }
}