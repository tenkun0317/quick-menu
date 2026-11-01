package xyz.inorganic.quickmenu.other

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object ModKeybindings {
    lateinit var menuOpenKeybinding: KeyMapping
        private set
    lateinit var moveModifierKeybind: KeyMapping
        private set
    lateinit var deleteModifierKeybind: KeyMapping
        private set
    lateinit var searchKeybind: KeyMapping
        private set
    lateinit var editModeKeybind: KeyMapping
        private set
    lateinit var navigateBackKeybind: KeyMapping
        private set
    lateinit var closeSearchKeybind: KeyMapping
        private set
    lateinit var radialOpenKeybinding: KeyMapping
        private set
    lateinit var macroStopKeybinding: KeyMapping
        private set

    fun initialize() {
        val mainCategory = KeyMapping.Category(
            Identifier.fromNamespaceAndPath("quickmenu", "main")
        )

        fun register(name: String, defaultKey: Int): KeyMapping =
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, mainCategory)
            )

        menuOpenKeybinding = register("key.quickmenu.open", GLFW.GLFW_KEY_G)
        moveModifierKeybind = register("key.quickmenu.move_modifier", GLFW.GLFW_KEY_LEFT_CONTROL)
        deleteModifierKeybind = register("key.quickmenu.delete_modifier", GLFW.GLFW_KEY_LEFT_SHIFT)
        searchKeybind = register("key.quickmenu.search", GLFW.GLFW_KEY_F)
        editModeKeybind = register("key.quickmenu.edit_mode", GLFW.GLFW_KEY_E)
        navigateBackKeybind = register("key.quickmenu.navigate_back", GLFW.GLFW_KEY_BACKSPACE)
        closeSearchKeybind = register("key.quickmenu.close_search", GLFW.GLFW_KEY_ESCAPE)
        radialOpenKeybinding = register("key.quickmenu.radial_open", GLFW.GLFW_KEY_R)
        macroStopKeybinding = register("key.quickmenu.macro_stop", GLFW.GLFW_KEY_K)
    }
}
