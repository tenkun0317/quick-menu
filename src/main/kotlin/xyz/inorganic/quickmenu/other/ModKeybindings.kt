package xyz.inorganic.quickmenu.other

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object ModKeybindings {
    lateinit var menuOpenKeybinding: KeyMapping

    fun initialize() {
        val mainCategory = KeyMapping.Category.register(
            Identifier.parse("quickmenu:main")
        )

        menuOpenKeybinding = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.quickmenu.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                mainCategory
            )
        )
    }
}
