package xyz.imcodist.quickmenu.data

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import xyz.imcodist.quickmenu.QuickMenu
import xyz.imcodist.quickmenu.data.command_actions.ActionData
import xyz.imcodist.quickmenu.other.ModConfig

class ActionButtonData(
    var name: String = "",
    var actions: MutableList<ActionData> = mutableListOf(),
    var icon: ItemStack = ItemStack.EMPTY,
    var keybind: MutableList<Int> = mutableListOf()
) {
    var keyPressed = false

    fun getKey(): InputConstants.Key? {
        if (keybind.size < 4) return null
        return InputConstants.Type.KEYSYM.getOrCreate(keybind[0])
    }

    fun run(isKeybind: Boolean = false) {
        val displayRunText = QuickMenu.CONFIG.displayRunText()
        if (displayRunText == ModConfig.DisplayRunText.ALWAYS || 
            (displayRunText == ModConfig.DisplayRunText.KEYBIND_ONLY && isKeybind)) {
            val client = Minecraft.getInstance()
            client.player?.displayClientMessage(Component.literal("Ran action \"$name\""), true)
        }

        actions.forEach { it.run() }
    }

    // Helper for CustomModelData
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
