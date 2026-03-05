package xyz.inorganic.quickmenu.logic

import net.minecraft.client.Minecraft
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.KeybindHandler
import xyz.inorganic.quickmenu.other.ModKeybindings
import xyz.inorganic.quickmenu.ui.MainUI
import org.lwjgl.glfw.GLFW
import com.mojang.blaze3d.platform.InputConstants

object KeybindManager {
    private var menuKeyPressed = false

    fun onClientTick(client: Minecraft) {
        handleMenuKey(client)
        
        if (client.screen == null) {
            handleActionKeys(client)
        }

        KeybindHandler.runQueue()
    }

    private fun handleMenuKey(client: Minecraft) {
        if (ModKeybindings.menuOpenKeybinding.isDown) {
            if (!menuKeyPressed) {
                client.setScreen(MainUI())
            }
            menuKeyPressed = true
        } else if (client.screen == null) {
            menuKeyPressed = false
        }
    }

    private fun handleActionKeys(client: Minecraft) {
        ActionButtonDataHandler.actions.forEach { actionData ->
            var shouldRun = false
            val keybind = actionData.keybind
            
            if (keybind.size >= 4) {
                if (keybind[3] == 0) {
                    // Keyboard
                    actionData.getKey()?.let { key ->
                        // isKeyDown takes Window object in 1.21.x
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
                    // Mouse
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
