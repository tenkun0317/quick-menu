package xyz.inorganic.quickmenu.ui

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import com.mojang.blaze3d.platform.InputConstants
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.ModKeybindings
import xyz.inorganic.quickmenu.other.ModMenuIntegration
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton
import java.util.Collections
import kotlin.math.ceil

class MainUI : Screen(Component.translatable("menu.main.title")) {
    var editMode = false
    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 0
    private var menuHeight = 0
    
    private var scrollOffset = 0
    private val rowHeight = 30

    private val buttonDataMap = mutableMapOf<QuickMenuButton, ActionButtonData>()
    private var isDraggingScrollbar = false

    override fun init() {
        val config = QuickMenu.CONFIG
        val colCount = config.buttonsPerRow
        val rowCount = config.visibleRows
        
        menuWidth = (colCount * 30) + 16
        menuHeight = 24 + (rowCount * 30) + 5
        
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        buttonDataMap.clear()

        if (!config.hideEditIcon) {
            addRenderableWidget(Button.builder(Component.literal(if (editMode) "×" else "✎")) {
                editMode = !editMode
                rebuildWidgets()
            }.pos(menuX + menuWidth - 22, menuY + 4).size(18, 18).build())
        }

        val actions = ActionButtonDataHandler.actions
        val startX = menuX + 10
        val startY = menuY + 28
        val visibleAreaHeight = rowCount * rowHeight

        actions.forEachIndexed { index, data ->
            val row = index / colCount
            val col = index % colCount
            val btnX = startX + col * 30
            val btnY = startY + row * rowHeight - scrollOffset

            if (btnY >= startY && btnY + 26 <= startY + visibleAreaHeight) {
                val button = QuickMenuButton(data.icon, { handleLeftClick(data) }, { handleRightClick(data) })
                button.x = btnX
                button.y = btnY
                button.setTooltip(Tooltip.create(Component.literal(data.name)))
                addRenderableWidget(button)
                buttonDataMap[button] = data
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

    private fun isMouseOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val config = QuickMenu.CONFIG
        val totalRows = ceil(ActionButtonDataHandler.actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        if (totalRows <= config.visibleRows) return false

        val sbX = menuX + menuWidth - 6
        val sbY = menuY + 28
        val sbHeight = config.visibleRows * rowHeight
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= sbY && mouseY <= sbY + sbHeight
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val config = QuickMenu.CONFIG
        val totalRows = ceil(ActionButtonDataHandler.actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        val visibleRows = config.visibleRows
        val totalContentHeight = totalRows * rowHeight
        val visibleHeight = visibleRows * rowHeight
        val maxScroll = maxOf(0, totalContentHeight - visibleHeight)
        
        val sbY = menuY + 28
        val sbHeight = visibleHeight.toDouble()
        
        val percentage = ((mouseY - sbY) / sbHeight).coerceIn(0.0, 1.0)
        scrollOffset = (percentage * maxScroll).toInt()
        scrollOffset = (scrollOffset / rowHeight) * rowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        
        rebuildWidgets()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (isMouseOverScrollbar(event.x(), event.y())) {
            isDraggingScrollbar = true
            updateScrollFromMouse(event.y())
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(event.y())
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val config = QuickMenu.CONFIG
        val totalRows = ceil(ActionButtonDataHandler.actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        val maxScroll = maxOf(0, (totalRows - config.visibleRows) * rowHeight)
        scrollOffset = (scrollOffset - (verticalAmount * rowHeight).toInt()).coerceIn(0, maxScroll)
        rebuildWidgets()
        return true
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        renderThinBorder(guiGraphics, menuX, menuY, menuWidth, menuHeight, 0x33FFFFFF.toInt())
        
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 24, 0x22FFFFFF.toInt())
        val separatorY = menuY + 24
        guiGraphics.fill(menuX + 1, separatorY, menuX + menuWidth - 1, separatorY + 1, 0x44FFFFFF.toInt())

        super.render(guiGraphics, mouseX, mouseY, partialTick)
        
        // --- Scrollbar Drawing ---
        val config = QuickMenu.CONFIG
        val totalRows = ceil(ActionButtonDataHandler.actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        if (totalRows > config.visibleRows) {
            val sbX = menuX + menuWidth - 5
            val sbY = menuY + 28
            val sbHeight = config.visibleRows * rowHeight
            
            guiGraphics.fill(sbX, sbY, sbX + 3, sbY + sbHeight, 0x22FFFFFF.toInt())
            
            val thumbHeight = maxOf(4, (config.visibleRows.toDouble() / totalRows.toDouble() * sbHeight).toInt())
            val maxScroll = (totalRows - config.visibleRows) * rowHeight
            val thumbY = if (maxScroll > 0) {
                sbY + (scrollOffset.toDouble() / maxScroll.toDouble() * (sbHeight - thumbHeight)).toInt()
            } else sbY
            
            val thumbColor = if (isDraggingScrollbar || isMouseOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            guiGraphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbHeight, thumbColor)
        }

        val contentStartY = menuY + 25
        val contentHeight = config.visibleRows * rowHeight
        val contentEndY = contentStartY + contentHeight + 3
        val maxScroll = maxOf(0, (totalRows - config.visibleRows) * rowHeight)

        if (scrollOffset > 0) guiGraphics.fillGradient(menuX + 1, contentStartY, menuX + menuWidth - 1, contentStartY + 12, 0x99000000.toInt(), 0x00000000.toInt())
        if (scrollOffset < maxScroll) guiGraphics.fillGradient(menuX + 1, contentEndY - 12, menuX + menuWidth - 1, contentEndY, 0x00000000.toInt(), 0x99000000.toInt())

        guiGraphics.drawString(font, title, menuX + 10, menuY + 8, -1, true)

        if (ActionButtonDataHandler.actions.isEmpty()) {
            val emptyMsg = Component.translatable("menu.main.no_actions")
            val msgW = font.width(emptyMsg)
            guiGraphics.drawString(font, emptyMsg, menuX + (menuWidth - msgW) / 2, menuY + (menuHeight / 2), 0x66FFFFFF.toInt(), false)
        }

        if (editMode) {
            val window = Minecraft.getInstance().window
            val isShiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
            val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)

            buttonDataMap.keys.forEach { btn ->
                if (btn.isHovered) {
                    if (isShiftDown) renderIndicator(guiGraphics, btn, 0xFFFF0000.toInt(), "×")
                    else if (isCtrlDown) renderIndicator(guiGraphics, btn, 0xFF00AAFF.toInt(), "↔")
                }
            }
        }
    }

    private fun renderIndicator(guiGraphics: GuiGraphics, btn: QuickMenuButton, color: Int, text: String) {
        val xSize = 10
        val xX = btn.x + btn.width - xSize + 2
        val xY = btn.y - 2
        guiGraphics.fill(xX, xY, xX + xSize, xY + xSize, color)
        guiGraphics.drawString(font, text, xX + (xSize - font.width(text)) / 2 + 1, xY + 1, -1, false)
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
            if (isShiftDown) deleteAction(data)
            else if (isCtrlDown) moveAction(data, -1)
            else gotoActionEditor(data)
            return
        }
        data.run()
        if (QuickMenu.CONFIG.closeOnAction) minecraft?.setScreen(null)
    }

    private fun handleRightClick(data: ActionButtonData) {
        if (!editMode) return
        val window = Minecraft.getInstance().window
        val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
        if (isCtrlDown) moveAction(data, 1)
        else deleteAction(data)
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

    private fun handleReleaseAction() {
        val hoveredBtn = buttonDataMap.keys.find { it.isHovered }
        hoveredBtn?.let { btn ->
            val data = buttonDataMap[btn]
            if (data != null) handleLeftClick(data)
        }
        minecraft?.setScreen(null)
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
                handleReleaseAction()
                return true
            }
        }
        return super.keyReleased(event)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        isDraggingScrollbar = false
        if (!editMode && QuickMenu.CONFIG.closeOnKeyReleased) {
            if (ModKeybindings.menuOpenKeybinding.matchesMouse(event)) {
                handleReleaseAction()
                return true
            }
        }
        return super.mouseReleased(event)
    }

    override fun isPauseScreen(): Boolean = false
}
