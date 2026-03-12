package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.components.EditBox
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
import xyz.inorganic.quickmenu.ui.popups.BreadcrumbPopupUI
import java.util.Collections
import kotlin.math.ceil

class MainUI : Screen(Component.translatable("menu.main.title")) {
    var editMode = false
    private var isSearching = false
    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 0
    private var menuHeight = 0
    
    private var scrollOffset = 0
    private val rowHeight = 30

    private lateinit var searchBox: EditBox
    private val buttonDataMap = mutableMapOf<QuickMenuButton, ActionButtonData>()
    private var isDraggingScrollbar = false
    private var firstInit = true

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
        
        // Reset navigation ONLY on initial open if keepNavigationHistory is false
        if (firstInit) {
            if (!config.keepNavigationHistory && navigationStack.isNotEmpty()) {
                navigateRoot()
            }
            firstInit = false
        }

        menuWidth = config.buttonsPerRow * 30 + 16
        menuHeight = 24 + config.visibleRows * 30 + 5
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        buttonDataMap.clear()

        // Search Box
        if (isSearching) {
            val existingValue = if (::searchBox.isInitialized) searchBox.value else ""
            searchBox = EditBox(font, menuX + 8, menuY + 6, menuWidth - 50, 12, Component.empty())
            searchBox.isBordered = false
            searchBox.value = existingValue
            searchBox.setResponder { 
                scrollOffset = 0
                rebuildWidgets() 
            }
            addRenderableWidget(searchBox)
            setInitialFocus(searchBox)
        }

        // Toggle Buttons
        val toggleButtonsY = menuY + 4
        var currentToggleX = menuX + menuWidth - 22

        if (!config.hideEditIcon) {
            addRenderableWidget(Button.builder(Component.literal(if (editMode) "×" else "✎")) {
                editMode = !editMode
                rebuildWidgets()
            }.pos(currentToggleX, toggleButtonsY).size(18, 18).build())
            currentToggleX -= 20
        }

        addRenderableWidget(Button.builder(Component.literal(if (isSearching) "⌫" else "🔍")) {
            isSearching = !isSearching
            if (!isSearching && ::searchBox.isInitialized) searchBox.value = ""
            rebuildWidgets()
        }.pos(currentToggleX, toggleButtonsY).size(18, 18).build())

        val actions = if (isSearching && ::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            getFilteredActions(searchBox.value)
        } else {
            currentFolder()?.children ?: ActionButtonDataHandler.actions
        }

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

    private fun getFilteredActions(query: String): List<ActionButtonData> {
        val result = mutableListOf<ActionButtonData>()
        fun collect(actions: List<ActionButtonData>) {
            for (action in actions) {
                if (action.name.contains(query, ignoreCase = true)) {
                    result.add(action)
                }
                if (action.isFolder) {
                    collect(action.children)
                }
            }
        }
        collect(ActionButtonDataHandler.actions)
        return result
    }

    override fun rebuildWidgets() {
        clearWidgets()
        init()
    }

    private fun isMouseOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val config = QuickMenu.CONFIG
        val actions = if (isSearching && ::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            getFilteredActions(searchBox.value)
        } else {
            currentFolder()?.children ?: ActionButtonDataHandler.actions
        }
        val totalRows = ceil(actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        if (totalRows <= config.visibleRows) return false
        val sbX = menuX + menuWidth - 6
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= menuY + 28 && mouseY <= menuY + 28 + (config.visibleRows * rowHeight)
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val config = QuickMenu.CONFIG
        val actions = if (isSearching && ::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            getFilteredActions(searchBox.value)
        } else {
            currentFolder()?.children ?: ActionButtonDataHandler.actions
        }
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
        if (!isSearching) {
            val y = menuY + 8
            val breadcrumbs = getBreadcrumbs()
            breadcrumbs.forEach { (label, level, bounds) ->
                if (event.x() >= bounds.first && event.x() <= bounds.second && event.y() >= y && event.y() <= y + 9) {
                    if (level == -2) { // "..."
                        val visibleLevels = breadcrumbs.map { it.level }.toSet()
                        val omitted = navigationStack.mapIndexedNotNull { index, data ->
                            if (!visibleLevels.contains(index)) index to data.name else null
                        }
                        if (omitted.isNotEmpty()) {
                            minecraft?.setScreen(BreadcrumbPopupUI(omitted, { 
                                navigateToLevel(it)
                                scrollOffset = 0
                                rebuildWidgets()
                            }, this))
                        }
                        return true
                    } else {
                        navigateToLevel(level)
                        scrollOffset = 0
                        rebuildWidgets()
                        return true
                    }
                }
            }
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
        val actions = if (isSearching && ::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            getFilteredActions(searchBox.value)
        } else {
            currentFolder()?.children ?: ActionButtonDataHandler.actions
        }
        val totalRows = ceil(actions.size.toDouble() / config.buttonsPerRow.toDouble()).toInt()
        val maxScroll = maxOf(0, (totalRows * rowHeight) - (config.visibleRows * rowHeight))
        scrollOffset = (scrollOffset - (verticalAmount * rowHeight).toInt()).coerceIn(0, maxScroll)
        rebuildWidgets()
        return true
    }

    private fun isKeyDown(keyName: String): Boolean {
        val window = Minecraft.getInstance().window
        return try {
            val key = InputConstants.getKey(keyName)
            if (key == InputConstants.UNKNOWN) return false
            
            if (key.type == InputConstants.Type.MOUSE) {
                when (key.value) {
                    0 -> Minecraft.getInstance().mouseHandler.isLeftPressed
                    1 -> Minecraft.getInstance().mouseHandler.isRightPressed
                    2 -> Minecraft.getInstance().mouseHandler.isMiddlePressed
                    else -> false
                }
            } else {
                InputConstants.isKeyDown(window, key.value)
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        renderThinBorder(guiGraphics, menuX, menuY, menuWidth, menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 24, 0x22FFFFFF.toInt())
        val separatorY = menuY + 24
        guiGraphics.fill(menuX + 1, separatorY, menuX + menuWidth - 1, separatorY + 1, 0x44FFFFFF.toInt())

        super.render(guiGraphics, mouseX, mouseY, partialTick)
        
        val actions = if (isSearching && ::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            getFilteredActions(searchBox.value)
        } else {
            currentFolder()?.children ?: ActionButtonDataHandler.actions
        }
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

        if (!isSearching) {
            renderBreadcrumbs(guiGraphics, mouseX, mouseY)
        } else {
            // Draw search focus underline
            if (::searchBox.isInitialized && searchBox.isFocused) {
                guiGraphics.fill(menuX + 8, menuY + 18, menuX + menuWidth - 42, menuY + 19, 0xAAFFFFFF.toInt())
            }
        }

        if (actions.isEmpty()) {
            val emptyMsg = Component.translatable("menu.main.no_actions")
            val msgW = font.width(emptyMsg)
            guiGraphics.drawString(font, emptyMsg, menuX + (menuWidth - msgW) / 2, menuY + (menuHeight / 2), 0x66FFFFFF.toInt(), false)
        }

        if (editMode) {
            val isDeleteDown = isKeyDown(QuickMenu.CONFIG.deleteModifier)
            val isMoveDown = isKeyDown(QuickMenu.CONFIG.moveModifier)

            buttonDataMap.keys.forEach { btn ->
                if (btn.isHovered) {
                    if (isDeleteDown) renderIndicator(guiGraphics, btn, 0xFFFF0000.toInt(), "×")
                    else if (isMoveDown) renderIndicator(guiGraphics, btn, 0xFF00AAFF.toInt(), "↔")
                }
            }
        }
    }

    data class BreadcrumbItem(val label: String, val level: Int, val bounds: Pair<Int, Int>)

    private fun getBreadcrumbs(): List<BreadcrumbItem> {
        val maxWidth = menuWidth - 30 // Padding for icons
        var currentX = menuX + 10
        
        // 1. Calculate full path widths
        val rootWidth = font.width("Root")
        var totalWidth = currentX + rootWidth + 5
        
        val allItems = navigationStack.mapIndexed { index, data ->
            val label = data.name
            val w = font.width("> $label")
            val itemWidth = w + 5
            totalWidth += itemWidth
            Triple(label, index, w)
        }
        
        // 2. If it fits, return full path
        if (totalWidth <= menuX + maxWidth) {
            val result = mutableListOf<BreadcrumbItem>()
            var x = currentX
            result.add(BreadcrumbItem("Root", -1, x to (x + rootWidth)))
            x += rootWidth + 5
            allItems.forEach { (label, index, w) ->
                result.add(BreadcrumbItem(label, index, x to (x + w)))
                x += w + 5
            }
            return result
        }
        
        // 3. Truncation logic: Root > ... > [as many as fit from end]
        val result = mutableListOf<BreadcrumbItem>()
        var x = currentX
        
        // Add Root
        result.add(BreadcrumbItem("Root", -1, x to (x + rootWidth)))
        x += rootWidth + 5
        
        // Add "..."
        val dotsW = font.width("> ...")
        result.add(BreadcrumbItem("...", -2, x to (x + dotsW)))
        x += dotsW + 5
        
        // Add trailing items greedily from the end
        val availableWidth = (menuX + maxWidth) - x
        val trailingItems = mutableListOf<BreadcrumbItem>()
        var usedTrailingWidth = 0
        
        for (i in allItems.indices.reversed()) {
            val (label, index, w) = allItems[i]
            if (usedTrailingWidth + w + 5 <= availableWidth) {
                trailingItems.add(0, BreadcrumbItem(label, index, 0 to 0)) // Bounds set below
                usedTrailingWidth += w + 5
            } else {
                break
            }
        }
        
        // If even the last item doesn't fit, force show it (better than nothing)
        if (trailingItems.isEmpty() && allItems.isNotEmpty()) {
            val (label, index, w) = allItems.last()
            trailingItems.add(BreadcrumbItem(label, index, 0 to 0))
        }
        
        // Calculate final bounds for trailing items
        trailingItems.forEach { item ->
            val w = font.width("> ${item.label}")
            val finalItem = item.copy(bounds = x to (x + w))
            result.add(finalItem)
            x += w + 5
        }
        
        return result
    }

    private fun renderBreadcrumbs(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val y = menuY + 8
        val breadcrumbs = getBreadcrumbs()
        
        breadcrumbs.forEach { (label, level, bounds) ->
            val isRoot = level == -1
            val isDots = level == -2
            val displayText = if (isRoot) label else "> $label"
            
            val isHovered = mouseX >= bounds.first && mouseX <= bounds.second && mouseY >= y && mouseY <= y + 9
            val isLast = level == navigationStack.size - 1
            
            val color = when {
                isDots -> 0xFF666666.toInt()
                isLast -> -1
                isHovered -> 0xFFFFFFFF.toInt()
                else -> 0xFFAAAAAA.toInt()
            }
            
            guiGraphics.drawString(font, displayText, bounds.first, y, color, true)
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
        if (editMode) {
            if (isKeyDown(QuickMenu.CONFIG.deleteModifier)) {
                deleteAction(data)
            } else if (isKeyDown(QuickMenu.CONFIG.moveModifier)) {
                moveAction(data, -1)
            } else {
                if (data.isFolder) {
                    if (isSearching) {
                        isSearching = false
                        if (::searchBox.isInitialized) searchBox.value = ""
                    }
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
            if (isSearching) {
                isSearching = false
                if (::searchBox.isInitialized) searchBox.value = ""
            }
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
        
        if (isKeyDown(QuickMenu.CONFIG.moveModifier)) {
            moveAction(data, 1)
        } else {
            // Right click ALWAYS opens editor in edit mode
            gotoActionEditor(data)
        }
    }

    private fun moveAction(data: ActionButtonData, direction: Int) {
        if (isSearching) return // Disable move in search mode
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
        // If searching, we need to find the actual parent to delete from
        if (isSearching) {
            fun findAndDelete(list: MutableList<ActionButtonData>): Boolean {
                if (list.remove(data)) return true
                for (action in list) {
                    if (action.isFolder && findAndDelete(action.children)) return true
                }
                return false
            }
            findAndDelete(ActionButtonDataHandler.actions)
        } else {
            val actions = currentFolder()?.children ?: ActionButtonDataHandler.actions
            actions.remove(data)
        }
        
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
        if (event.key() == GLFW.GLFW_KEY_F && (event.modifiers() and GLFW.GLFW_MOD_CONTROL != 0)) {
            isSearching = !isSearching
            if (!isSearching && ::searchBox.isInitialized) searchBox.value = ""
            rebuildWidgets()
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_E) {
            editMode = !editMode
            rebuildWidgets()
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && currentFolder() != null && !isSearching) {
            navigateToLevel(navigationStack.size - 2)
            scrollOffset = 0
            rebuildWidgets()
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && isSearching) {
            isSearching = false
            if (::searchBox.isInitialized) searchBox.value = ""
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
