package xyz.inorganic.quickmenu.logic

import net.minecraft.client.Minecraft
import xyz.inorganic.quickmenu.mixins.KeyBindingMixin
import xyz.inorganic.quickmenu.macros.MacroExecutor
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.ActionExecutor
import xyz.inorganic.quickmenu.other.KeybindHandler
import xyz.inorganic.quickmenu.other.ModKeybindings
import xyz.inorganic.quickmenu.ui.MainUI
import xyz.inorganic.quickmenu.ui.RadialMenuUI
import org.lwjgl.glfw.GLFW
import com.mojang.blaze3d.platform.InputConstants

object KeybindManager {
    private val MODIFIER_KEY_CODES = setOf(
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT
    )

    private var menuKeyPressed = false
    private var radialKeyPressed = false
    private var macroStopKeyPressed = false
    private var radialHoveredAction: xyz.inorganic.quickmenu.data.ActionButtonData? = null

    fun onClientTick(client: Minecraft) {
        handleMenuKey(client)
        handleRadialKey(client)
        handleMacroStopKey(client)
        
        if (client.gui.screen() == null) {
            handleActionKeys(client)
        }

        KeybindHandler.runQueue()

        ActionExecutor.advance()

        MacroExecutor.advanceAll()
    }

    private fun handleMenuKey(client: Minecraft) {
        val rawKey = (ModKeybindings.menuOpenKeybinding as KeyBindingMixin).key
        val isDown = InputConstants.isKeyDown(client.window, rawKey.value)

        if (isDown) {
            if (!menuKeyPressed) {
                menuKeyPressed = true
                if (client.gui.screen() is MainUI) {
                    client.gui.setScreen(null)
                } else if (client.gui.screen() == null && !isModifierHeld(rawKey, client)) {
                    client.gui.setScreen(MainUI())
                }
            }
        } else {
            menuKeyPressed = false
        }
    }

    private fun handleRadialKey(client: Minecraft) {
        val rawKey = (ModKeybindings.radialOpenKeybinding as KeyBindingMixin).key
        val isDown = InputConstants.isKeyDown(client.window, rawKey.value)

        if (isDown) {
            if (!radialKeyPressed && client.gui.screen() == null && !isModifierHeld(rawKey, client)) {
                client.gui.setScreen(RadialMenuUI())
            }
            radialKeyPressed = true
        } else {
            if (client.gui.screen() is RadialMenuUI) {
                val screen = client.gui.screen() as RadialMenuUI
                screen.handleKeyRelease()
            }
            radialKeyPressed = false
        }
    }

    private fun handleMacroStopKey(client: Minecraft) {
        val rawKey = (ModKeybindings.macroStopKeybinding as KeyBindingMixin).key
        val isDown = InputConstants.isKeyDown(client.window, rawKey.value)

        if (isDown && !macroStopKeyPressed) {
            if (!isModifierHeld(rawKey, client)) {
                MacroExecutor.emergencyStop()
            }
            macroStopKeyPressed = true
        } else if (!isDown) {
            macroStopKeyPressed = false
        }
    }

    private fun isModifierHeld(boundKey: InputConstants.Key, client: Minecraft): Boolean {
        if (boundKey.type == InputConstants.Type.KEYSYM &&
            boundKey.value in MODIFIER_KEY_CODES
        ) {
            return false
        }
        val w = client.window
        return InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL) ||
            InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL) ||
            InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT) ||
            InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_ALT) ||
            InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    private fun handleActionKeys(client: Minecraft) {
        ActionButtonDataHandler.actions.forEach { actionData ->
            var shouldRun = false
            val keybind = actionData.keybind
            
            if (keybind.size >= 4) {
                if (keybind[3] == 0) {
                    actionData.getKey()?.let { key ->
                        if (InputConstants.isKeyDown(client.window, key.value)) {
                            val mods = keybind[2]
                            val reqCtrl = (mods and GLFW.GLFW_MOD_CONTROL != 0)
                            val reqShift = (mods and GLFW.GLFW_MOD_SHIFT != 0)
                            val reqAlt = (mods and GLFW.GLFW_MOD_ALT != 0)
                            
                            val isCtrlDown = InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                            val isShiftDown = InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_RIGHT_SHIFT)
                            val isAltDown = InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(client.window, GLFW.GLFW_KEY_RIGHT_ALT)
                            
                            if (reqCtrl == isCtrlDown && reqShift == isShiftDown && reqAlt == isAltDown) {
                                if (!actionData.keyPressed) shouldRun = true
                                actionData.keyPressed = true
                            } else {
                                actionData.keyPressed = false
                            }
                        } else {
                            actionData.keyPressed = false
                        }
                    }
                } else {
                    val pressed = when (keybind[0]) {
                        0 -> client.mouseHandler.isLeftPressed
                        1 -> client.mouseHandler.isRightPressed
                        2 -> client.mouseHandler.isMiddlePressed
                        else -> false
                    }
                    if (pressed) {
                        if (!actionData.keyPressed) shouldRun = true
                        actionData.keyPressed = true
                    } else {
                        actionData.keyPressed = false
                    }
                }
            }

            if (shouldRun) {
                actionData.run(isKeybind = true)
            }
        }
    }
}
