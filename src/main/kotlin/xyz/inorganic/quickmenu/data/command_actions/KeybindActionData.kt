package xyz.inorganic.quickmenu.data.command_actions

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.other.KeybindHandler

@Serializable
class KeybindActionData(
    val translationKey: String = ""
) : ActionData() {
    override val type: String = "key"
    override val value: String = translationKey
    
    override val typeString: String = "KEY"
    override fun getDisplayString(): String = Component.translatable(translationKey).string
    
    override fun run() {
        KeybindHandler.pressKey(translationKey)
    }
}
