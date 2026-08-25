package xyz.inorganic.quickmenu.macros

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.MacroActionData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler

object MacroRegistry {

    fun resolve(name: String): MacroActionData? {
        return findInList(ActionButtonDataHandler.actions, name)
    }

    private fun findInList(list: List<ActionButtonData>, name: String): MacroActionData? {
        for (button in list) {
            if (button.name == name) {
                val macro = button.actions.filterIsInstance<MacroActionData>().firstOrNull()
                if (macro != null) return macro
            }
            val child = findInList(button.children, name)
            if (child != null) return child
        }
        return null
    }
}