package xyz.inorganic.quickmenu.data

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.other.ModConfig

class ActionButtonData(
    var name: String = "",
    var actions: MutableList<ActionData> = mutableListOf(),
    var icon: ItemStack = ItemStack.EMPTY,
    var keybind: MutableList<Int> = mutableListOf(),
    var isFolder: Boolean = false,
    var children: MutableList<ActionButtonData> = mutableListOf()
) {
    var keyPressed = false

    fun getKey(): InputConstants.Key? {
        if (keybind.size < 4) return null
        return InputConstants.Type.KEYSYM.getOrCreate(keybind[0])
    }

    fun run(isKeybind: Boolean = false) {
        // Folders don't "run" actions in the traditional sense when clicked in the menu,
        // but they might have actions associated with them for keybinds.
        if (isFolder && !isKeybind) return

        val displayRunText = QuickMenu.CONFIG.displayRunText
        if (displayRunText == ModConfig.DisplayRunText.ALWAYS || 
            (displayRunText == ModConfig.DisplayRunText.KEYBIND_ONLY && isKeybind)) {
            val client = Minecraft.getInstance()
            client.player?.displayClientMessage(Component.literal("Ran action \"$name\""), true)
        }

        actions.forEach { it.run() }
    }

    class CustomModelDataValues(cmdStr: String) {
        val stringList = listOf(cmdStr)
        var floatList = listOf<Float>()

        init {
            try {
                floatList = listOf(cmdStr.toFloat())
            } catch (ignored: Exception) {}
        }

        fun getComponent(): CustomModelData {
            return CustomModelData(floatList, emptyList(), stringList, emptyList())
        }
    }
}
