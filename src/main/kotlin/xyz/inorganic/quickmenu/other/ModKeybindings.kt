package xyz.inorganic.quickmenu.other

import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object ModKeybindings {
    lateinit var menuOpenKeybinding: KeyMapping
    lateinit var moveModifierKeybind: KeyMapping
    lateinit var deleteModifierKeybind: KeyMapping
    lateinit var searchKeybind: KeyMapping
    lateinit var editModeKeybind: KeyMapping
    lateinit var navigateBackKeybind: KeyMapping
    lateinit var closeSearchKeybind: KeyMapping
    lateinit var radialOpenKeybinding: KeyMapping

    fun initialize() {
        val mainCategory = KeyMapping.Category(
            Identifier.fromNamespaceAndPath("quickmenu", "main")
        )

        menuOpenKeybinding = KeyMapping(
            "key.quickmenu.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            mainCategory
        )

        moveModifierKeybind = KeyMapping(
            "key.quickmenu.move_modifier",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            mainCategory
        )

        deleteModifierKeybind = KeyMapping(
            "key.quickmenu.delete_modifier",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            mainCategory
        )

        searchKeybind = KeyMapping(
            "key.quickmenu.search",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            mainCategory
        )

        editModeKeybind = KeyMapping(
            "key.quickmenu.edit_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            mainCategory
        )

        navigateBackKeybind = KeyMapping(
            "key.quickmenu.navigate_back",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            mainCategory
        )

        closeSearchKeybind = KeyMapping(
            "key.quickmenu.close_search",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_ESCAPE,
            mainCategory
        )

        radialOpenKeybinding = KeyMapping(
            "key.quickmenu.radial_open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            mainCategory
        )
    }
}