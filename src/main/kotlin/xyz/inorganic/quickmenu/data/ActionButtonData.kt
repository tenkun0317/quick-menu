package xyz.inorganic.quickmenu.data

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.other.ModConfig

class ActionButtonData(
    var name: String = "",
    var actions: MutableList<ActionData> = mutableListOf(),
    icon: ItemStack = ItemStack.EMPTY,
    keybind: MutableList<Int> = mutableListOf(),
    var isFolder: Boolean = false,
    var children: MutableList<ActionButtonData> = mutableListOf(),
    var registeredForRadial: Boolean = false
) {
    var keyPressed = false

    var iconString: String? = null
    var customModelDataString: String? = null

    private var _icon: ItemStack = icon

    var icon: ItemStack
        get() {
            if (_icon.isEmpty && iconString != null) {
                try {
                    val parts = iconString!!.split(":", limit = 2)
                    val identifier = Identifier.fromNamespaceAndPath(parts[0], parts[1])
                    val item = BuiltInRegistries.ITEM.getValue(identifier)
                    _icon = item.defaultInstance.copy()
                    if (customModelDataString != null && customModelDataString!!.isNotEmpty()) {
                        val cmdValues = CustomModelDataValues(customModelDataString!!)
                        _icon.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, cmdValues.getComponent())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return _icon
        }
        set(value) {
            _icon = value
            iconString = null
        }

    var keybind: MutableList<Int> = keybind

    internal fun setIconFromLoad(icon: ItemStack, iconStr: String?, cmdStr: String?) {
        this._icon = icon
        this.iconString = iconStr
        this.customModelDataString = cmdStr
    }

    fun getKey(): InputConstants.Key? {
        if (keybind.size < 4) return null
        return InputConstants.Type.KEYSYM.getOrCreate(keybind[0])
    }

    fun run(isKeybind: Boolean = false) {
        if (isFolder && !isKeybind) return

        val displayRunText = QuickMenu.CONFIG.displayRunText
        if (displayRunText == ModConfig.DisplayRunText.ALWAYS || 
            (displayRunText == ModConfig.DisplayRunText.KEYBIND_ONLY && isKeybind)) {
            val client = Minecraft.getInstance()
            client.player?.sendSystemMessage(Component.literal("Ran action \"$name\""))
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
