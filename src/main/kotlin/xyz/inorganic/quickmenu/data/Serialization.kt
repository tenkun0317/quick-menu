package xyz.inorganic.quickmenu.data

import kotlinx.serialization.Serializable
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.data.command_actions.CommandActionData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import java.util.*

@Serializable
data class ActionButtonDataJSON(
    val name: String,
    val actions: List<List<String>> = emptyList(),
    val icon: String? = null,
    val customModelData: String? = null,
    val keybind: List<Int> = emptyList(),
    val isFolder: Boolean = false,
    val children: List<ActionButtonDataJSON> = emptyList()
) {
    fun toActionButtonData(): ActionButtonData {
        val data = ActionButtonData(
            name = name,
            keybind = keybind.toMutableList(),
            isFolder = isFolder
        )
        
        data.actions = actions.mapNotNull { actionList ->
            if (actionList.size < 2) return@mapNotNull null
            val type = actionList[0]
            val value = actionList[1]
            
            when (type) {
                "cmd" -> CommandActionData(value)
                "key" -> KeybindActionData(value)
                else -> null
            }
        }.toMutableList()
        
        if (icon != null) {
            try {
                val identifier = Identifier.parse(icon)
                val itemOptional = BuiltInRegistries.ITEM.get(identifier)
                var stack = ItemStack.EMPTY
                if (itemOptional.isPresent) {
                    stack = ItemStack(itemOptional.get())
                }
                if (customModelData != null && customModelData.isNotEmpty()) {
                    val cmdValues = ActionButtonData.CustomModelDataValues(customModelData)
                    stack.set(DataComponents.CUSTOM_MODEL_DATA, cmdValues.getComponent())
                }
                data.icon = stack
            } catch (ignored: Exception) {}
        }

        data.children = children.map { it.toActionButtonData() }.toMutableList()
        
        return data
    }
}

fun ActionButtonData.toJSON(): ActionButtonDataJSON {
    val actionList = actions.map { action ->
        listOf(action.type, action.value)
    }
    
    val iconStr = if (!icon.isEmpty) {
        BuiltInRegistries.ITEM.getKey(icon.item).toString()
    } else null
    
    val cmdStr = if (!icon.isEmpty) {
        val cmd = icon.get(DataComponents.CUSTOM_MODEL_DATA)
        if (cmd != null && cmd.strings().isNotEmpty()) cmd.strings()[0] else null
    } else null
    
    return ActionButtonDataJSON(
        name = name,
        actions = actionList,
        icon = iconStr,
        customModelData = cmdStr,
        keybind = keybind.toList(),
        isFolder = isFolder,
        children = children.map { it.toJSON() }
    )
}
