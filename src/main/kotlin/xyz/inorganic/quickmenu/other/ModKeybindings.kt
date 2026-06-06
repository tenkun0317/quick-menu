package xyz.inorganic.quickmenu.other

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object ModKeybindings {
    lateinit var menuOpenKeybinding: KeyMapping
    lateinit var moveModifierKeybind: KeyMapping
    lateinit var deleteModifierKeybind: KeyMapping

    fun initialize() {
        val mainCategory = KeyMapping.Category(
            Identifier.fromNamespaceAndPath("quickmenu", "main")
        )

        menuOpenKeybinding = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.quickmenu.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                mainCategory
            )
        )

        moveModifierKeybind = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.quickmenu.move_modifier",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                mainCategory
            )
        )

        deleteModifierKeybind = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.quickmenu.delete_modifier",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                mainCategory
            )
        )
    }
}