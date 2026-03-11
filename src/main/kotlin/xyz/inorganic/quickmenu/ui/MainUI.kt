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

    companion object {
        private val navigationStack = mutableListOf<ActionButtonData>()
        
        fun currentFolder(): ActionButtonData? = navigationStack.lastOrNull()
        fun navigateTo(folder: ActionButtonData) = navigationStack.add(folder)
        fun navigateToLevel(index: Int) {
            if (index == -1) navigationStack.clear()
            else while (navigationStack.size > index + 1) navigationStack.removeAt(navigationStack.size - 1)
        }
        fun navigateRoot() = navigationStack.clear()
    }

    override fun init() {
        val config = QuickMenu.CONFIG
        menuWidth = config.buttonsPerRow * 30 + 16
        menuHeight = 24 + config.visibleRows * 30 + 5
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        buttonDataMap.clear()

        if (!config.hideEditIcon) {
            addRenderableWidget(Button.builder(Component.literal(if (editMode) "×" else "✎")) {
                editMode = !editMode
                rebuildWidgets()
            }.pos(menuX + menuWidth - 22, menuY + 4).size(18, 18).build())
        }

        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val startX = menuX + 10
        val startY = menuY + 28
        val visibleAreaHeight = config.visibleRows * rowHeight

        actions.forEachIndexed { index, data ->
            val row = index / config.buttonsPerRow
            val col = index % config.buttonsPerRow
            val btnX = startX + col * 30
            val btnY = startY + row * rowHeight - scrollOffset

            if (btnY >= startY && btnY + 26 <= startY + visibleAreaHeight) {
                val button = QuickMenuButton(data.icon, { handleLeftClick(data) }, { handleRightClick(data) }, data.isFolder)
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
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val totalRows = ceil(actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        if (totalRows <= config.visibleRows) return false
        val sbX = menuX + menuWidth - 6
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= menuY + 28 && mouseY <= menuY + 28 + (config.visibleRows * rowHeight)
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val config = QuickMenu.CONFIG
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val totalRows = ceil(actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        val visibleH = config.visibleRows * rowHeight
        val maxScroll = maxOf(0, (totalRows * rowHeight) - visibleH)
        val sbY = menuY + 28
        val percentage = ((mouseY - sbY) / visibleH.toDouble()).coerceIn(0.0, 1.0)
        scrollOffset = ((percentage * maxScroll).toInt() / rowHeight) * rowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        rebuildWidgets()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (isMouseOverScrollbar(event.x(), event.y())) {
            isDraggingScrollbar = true
            updateScrollFromMouse(event.y())
            return true
        }

        // Check breadcrumb clicks
        var currentX = menuX + 10
        val y = menuY + 8
        val rootWidth = font.width("Root")
        if (event.x() >= currentX && event.x() <= currentX + rootWidth && event.y() >= y && event.y() <= y + 9) {
            navigateToLevel(-1)
            scrollOffset = 0
            rebuildWidgets()
            return true
        }
        currentX += rootWidth + 5
        navigationStack.forEachIndexed { index, data ->
            currentX += font.width(">") + 5
            val nameWidth = font.width(data.name)
            if (event.x() >= currentX && event.x() <= currentX + nameWidth && event.y() >= y && event.y() <= y + 9) {
                navigateToLevel(index)
                scrollOffset = 0
                rebuildWidgets()
                return true
            }
            currentX += nameWidth + 5
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val config = QuickMenu.CONFIG
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val totalRows = ceil(actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        val maxScroll = maxOf(0, (totalRows * rowHeight) - (config.visibleRows * rowHeight))
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
        
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val totalRows = ceil(actions.size.toDouble() / QuickMenu.CONFIG.buttonsPerRow.toDouble()).toInt()
        if (totalRows > QuickMenu.CONFIG.visibleRows) {
            val sbX = menuX + menuWidth - 5
            val sbY = menuY + 28
            val sbH = QuickMenu.CONFIG.visibleRows * rowHeight
            guiGraphics.fill(sbX, sbY, sbX + 3, sbY + sbH, 0x22FFFFFF.toInt())
            val thumbH = maxOf(4, (QuickMenu.CONFIG.visibleRows.toDouble() / totalRows.toDouble() * sbH).toInt())
            val maxScroll = (totalRows - QuickMenu.CONFIG.visibleRows) * rowHeight
            val thumbY = if (maxScroll > 0) sbY + (scrollOffset.toDouble() / maxScroll.toDouble() * (sbH - thumbH)).toInt() else sbY
            val thumbColor = if (isDraggingScrollbar || isMouseOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            guiGraphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, thumbColor)
        }

        val contentStartY = menuY + 25
        val contentEndY = contentStartY + QuickMenu.CONFIG.visibleRows * rowHeight + 3
        val maxScroll = maxOf(0, (totalRows - QuickMenu.CONFIG.visibleRows) * rowHeight)

        if (scrollOffset > 0) guiGraphics.fillGradient(menuX + 1, contentStartY, menuX + menuWidth - 1, contentStartY + 12, 0x99000000.toInt(), 0x00000000.toInt())
        if (scrollOffset < maxScroll) guiGraphics.fillGradient(menuX + 1, contentEndY - 12, menuX + menuWidth - 1, contentEndY, 0x00000000.toInt(), 0x99000000.toInt())

        renderBreadcrumbs(guiGraphics, mouseX, mouseY)

        if (actions.isEmpty()) {
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

    private fun renderBreadcrumbs(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        var currentX = menuX + 10
        val y = menuY + 8
        
        // Root
        val rootLabel = "Root"
        val rootWidth = font.width(rootLabel)
        val isRootHovered = mouseX >= currentX && mouseX <= currentX + rootWidth && mouseY >= y && mouseY <= y + 9
        val rootColor = if (navigationStack.isEmpty()) -1 else if (isRootHovered) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
        guiGraphics.drawString(font, rootLabel, currentX, y, rootColor, true)
        currentX += rootWidth + 5
        
        navigationStack.forEachIndexed { index, data ->
            // Separator
            guiGraphics.drawString(font, ">", currentX, y, 0xFF666666.toInt(), true)
            currentX += font.width(">") + 5
            
            // Folder name
            val nameWidth = font.width(data.name)
            val isHovered = mouseX >= currentX && mouseX <= currentX + nameWidth && mouseY >= y && mouseY <= y + 9
            val isLast = index == navigationStack.size - 1
            val color = if (isLast) -1 else if (isHovered) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
            guiGraphics.drawString(font, data.name, currentX, y, color, true)
            currentX += nameWidth + 5
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
            if (isShiftDown) {
                deleteAction(data)
            } else if (isCtrlDown) {
                moveAction(data, -1)
            } else {
                if (data.isFolder) {
                    navigateTo(data)
                    scrollOffset = 0
                    rebuildWidgets()
                } else {
                    gotoActionEditor(data)
                }
            }
            return
        }

        if (data.isFolder) {
            navigateTo(data)
            scrollOffset = 0
            rebuildWidgets()
        } else {
            data.run()
            if (QuickMenu.CONFIG.closeOnAction) minecraft?.setScreen(null)
        }
    }

    private fun handleRightClick(data: ActionButtonData) {
        if (!editMode) return
        val window = Minecraft.getInstance().window
        val isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
        
        if (isCtrlDown) {
            moveAction(data, 1)
        } else {
            // Right click ALWAYS opens editor in edit mode
            gotoActionEditor(data)
        }
    }

    private fun moveAction(data: ActionButtonData, direction: Int) {
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        val index = actions.indexOf(data)
        val newIndex = index + direction
        if (newIndex in 0 until actions.size) {
            Collections.swap(actions, index, newIndex)
            ActionButtonDataHandler.save()
            rebuildWidgets()
        }
    }

    private fun deleteAction(data: ActionButtonData) {
        val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
        actions.remove(data)
        ActionButtonDataHandler.save()
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
            if (data != null && !data.isFolder) handleLeftClick(data)
        }
        minecraft?.setScreen(null)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_E) {
            editMode = !editMode
            rebuildWidgets()
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && currentFolder() != null) {
            navigateToLevel(navigationStack.size - 2)
            scrollOffset = 0
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

    override fun isPauseScreen(): Boolean = false
}
