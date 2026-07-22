package xyz.inorganic.quickmenu.ui

import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.mixins.KeyBindingMixin

class KeyEventHandler(
    private val onSearchToggle: () -> Unit,
    private val onEditModeToggle: () -> Unit,
    private val onNavigateBack: () -> Unit,
    private val onSearchClear: () -> Unit,
    private val onRebuild: () -> Unit
) {
    data class KeyState(
        var isSearching: Boolean = false,
        var editMode: Boolean = false
    )

    private var keyState = KeyState()

    fun updateState(isSearching: Boolean, editMode: Boolean) {
        keyState.isSearching = isSearching
        keyState.editMode = editMode
    }

    fun handleKeyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        return when {
            event.key() == GLFW.GLFW_KEY_F -> {
                keyState.isSearching = !keyState.isSearching
                if (!keyState.isSearching) onSearchClear()
                onRebuild()
                true
            }
            event.key() == GLFW.GLFW_KEY_E -> {
                keyState.editMode = !keyState.editMode
                onRebuild()
                true
            }
            event.key() == GLFW.GLFW_KEY_BACKSPACE && NavigationState.currentFolder() != null && !keyState.isSearching -> {
                NavigationState.navigateBack()
                onRebuild()
                true
            }
            event.key() == GLFW.GLFW_KEY_ESCAPE && keyState.isSearching -> {
                keyState.isSearching = false
                onSearchClear()
                onRebuild()
                true
            }
            else -> false
        }
    }

    fun handleKeyReleased(event: net.minecraft.client.input.KeyEvent, closeOnKeyReleased: Boolean): Boolean {
        if (!keyState.editMode && closeOnKeyReleased) {
            val menuOpenKeybinding = xyz.inorganic.quickmenu.other.ModKeybindings.menuOpenKeybinding
            if (menuOpenKeybinding.matches(event)) {
                return true
            }
        }
        return false
    }

    fun isKeyMappingDown(keyMapping: KeyMapping): Boolean {
        val keyBindingMixin = keyMapping as KeyBindingMixin
        val key = keyBindingMixin.getKey()
        val client = Minecraft.getInstance()
        return if (key.type == InputConstants.Type.MOUSE) {
            when (key.value) {
                0 -> client.mouseHandler.isLeftPressed
                1 -> client.mouseHandler.isRightPressed
                2 -> client.mouseHandler.isMiddlePressed
                else -> false
            }
        } else {
            InputConstants.isKeyDown(client.window, key.value)
        }
    }
}