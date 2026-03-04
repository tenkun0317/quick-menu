package xyz.imcodist.quickmenu.ui

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import com.mojang.blaze3d.platform.InputConstants
import xyz.imcodist.quickmenu.QuickMenu
import xyz.imcodist.quickmenu.data.ActionButtonData
import xyz.imcodist.quickmenu.other.ActionButtonDataHandler
import xyz.imcodist.quickmenu.other.ModKeybindings
import xyz.imcodist.quickmenu.other.ModMenuIntegration
import xyz.imcodist.quickmenu.ui.components.QuickMenuButton
import java.util.Collections
import kotlin.math.ceil

class MainUI : Screen(Component.translatable("menu.main.title")) {
    var editMode = false
    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 0
    private var menuHeight = 0

    override fun init() {
        val config = QuickMenu.CONFIG
        menuWidth = config.menuWidth
        menuHeight = config.menuHeight
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        if (!config.hideEditIcon) {
            addRenderableWidget(Button.builder(Component.literal(if (editMode) "×" else "✎")) {
                editMode = !editMode
                rebuildWidgets()
            }.pos(menuX + menuWidth - 22, menuY + 4).size(18, 18).build())
        }

        val actions = ActionButtonDataHandler.actions
        val rowSize = config.buttonsPerRow
        val startX = menuX + 10
        val startY = menuY + 32

        actions.forEachIndexed { index, data ->
            val row = index / rowSize
            val col = index % rowSize
            val btnX = startX + col * 30
            val btnY = startY + row * 30

            if (btnY + 26 < menuY + menuHeight) {
                val button = QuickMenuButton(data.icon, {
                    handleLeftClick(data)
                }, {
                    handleRightClick(data)
                })
                button.x = btnX
                button.y = btnY
                button.setTooltip(Tooltip.create(Component.literal(data.name)))
                addRenderableWidget(button)
            }
        }

        if (editMode) {
            val editorY = menuY + menuHeight + 8
            addRenderableWidget(Button.builder(Component.literal("+ Action")) {
                gotoActionEditor(null)
            }.pos(menuX, editorY).size(menuWidth / 2 - 2, 20).build())

            addRenderableWidget(Button.builder(Component.literal("Settings")) {
                minecraft?.setScreen(ModMenuIntegration().getModConfigScreenFactory().create(this))
            }.pos(menuX + menuWidth / 2 + 2, editorY).size(menuWidth / 2 - 2, 20).build())
        }
    }

    override fun rebuildWidgets() {
        clearWidgets()
        init()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // --- 1. Background ---
        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        renderThinBorder(guiGraphics, menuX, menuY, menuWidth, menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 24, 0x22FFFFFF.toInt())
        guiGraphics.fill(menuX + 5, menuY + 23, menuX + menuWidth - 5, menuY + 24, 0x44FFFFFF.toInt())

        // --- 2. Widgets ---
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        
        // --- 3. Foreground Text ---
        guiGraphics.drawString(font, title, menuX + 10, menuY + 8, -1, true)

        if (ActionButtonDataHandler.actions.isEmpty()) {
            val emptyMsg = Component.translatable("menu.main.no_actions")
            val msgWidth = font.width(emptyMsg)
            guiGraphics.drawString(font, emptyMsg, menuX + (menuWidth - msgWidth) / 2, menuY + (menuHeight / 2), 0x66FFFFFF.toInt(), false)
        }

        // --- 4. Edit Mode Indicators ---
        if (editMode) {
            val window = Minecraft.getInstance().window
            val isShiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
            val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)

            children().filterIsInstance<QuickMenuButton>().forEach { btn ->
                if (btn.isHovered) {
                    if (isShiftDown) {
                        renderIndicator(guiGraphics, btn, 0xFFFF0000.toInt(), "×")
                    } else if (isCtrlDown) {
                        renderIndicator(guiGraphics, btn, 0xFF00AAFF.toInt(), "↔")
                    }
                }
            }
        }
    }

    private fun renderIndicator(guiGraphics: GuiGraphics, btn: QuickMenuButton, color: Int, text: String) {
        val xSize = 10
        val xX = btn.x + btn.width - xSize + 2
        val xY = btn.y - 2
        guiGraphics.fill(xX, xY, xX + xSize, xY + xSize, color)
        val textW = font.width(text)
        guiGraphics.drawString(font, text, xX + (xSize - textW) / 2 + 1, xY + 1, -1, false)
    }

    private fun renderThinBorder(guiGraphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        guiGraphics.fill(x, y, x + w, y + 1, color)
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color)
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, color)
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }

    private fun handleLeftClick(data: ActionButtonData) {
        val window = Minecraft.getInstance().window
        val isShiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
        val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)

        if (editMode) {
            if (isShiftDown) {
                deleteAction(data)
            } else if (isCtrlDown) {
                moveAction(data, -1)
            } else {
                gotoActionEditor(data)
            }
            return
        }

        data.run()
        if (QuickMenu.CONFIG.closeOnAction) minecraft?.setScreen(null)
    }

    private fun handleRightClick(data: ActionButtonData) {
        if (!editMode) return

        val window = Minecraft.getInstance().window
        val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)

        if (isCtrlDown) {
            moveAction(data, 1)
        } else {
            deleteAction(data)
        }
    }

    private fun moveAction(data: ActionButtonData, direction: Int) {
        val actions = ActionButtonDataHandler.actions
        val index = actions.indexOf(data)
        val newIndex = index + direction
        if (newIndex in 0 until actions.size) {
            Collections.swap(actions, index, newIndex)
            ActionButtonDataHandler.save()
            rebuildWidgets()
        }
    }

    private fun deleteAction(data: ActionButtonData) {
        ActionButtonDataHandler.remove(data)
        rebuildWidgets()
    }

    private fun gotoActionEditor(action: ActionButtonData?) {
        val actionEditor = ActionEditorUI(action)
        actionEditor.previousScreen = this
        minecraft?.setScreen(actionEditor)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_E) {
            editMode = !editMode
            rebuildWidgets()
            return true
        }
        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        if (!editMode && QuickMenu.CONFIG.closeOnKeyReleased) {
            if (ModKeybindings.menuOpenKeybinding.matches(event)) {
                minecraft?.setScreen(null)
                return true
            }
        }
        return super.keyReleased(event)
    }

    override fun isPauseScreen(): Boolean = false
}
