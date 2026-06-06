package xyz.inorganic.quickmenu.ui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ModKeybindings
import xyz.inorganic.quickmenu.other.ModMenuIntegration
import xyz.inorganic.quickmenu.ui.components.ActionButtonGrid
import xyz.inorganic.quickmenu.ui.components.BreadcrumbRenderer
import xyz.inorganic.quickmenu.ui.components.MenuScrollHandler
import xyz.inorganic.quickmenu.ui.components.SearchHandler
import xyz.inorganic.quickmenu.ui.popups.BreadcrumbPopupUI
import kotlin.math.ceil

class MainUI : Screen(Component.translatable("menu.main.title")) {
    var editMode = false
    private var isSearching = false
    private val firstInit = true

    private val menuWidth: Int get() = QuickMenu.CONFIG.buttonsPerRow * 30 + 16
    private val menuHeight: Int get() = 24 + QuickMenu.CONFIG.visibleRows * 30 + 5
    private val menuX: Int get() = (width - menuWidth) / 2
    private val menuY: Int get() = (height - menuHeight) / 2

    private val menuBackground by lazy { xyz.inorganic.quickmenu.ui.surfaces.MenuBackground(menuX, menuY, menuWidth, menuHeight) }
    private val scrollHandler by lazy { MenuScrollHandler(
        rowHeight = 30,
        visibleRows = { QuickMenu.CONFIG.visibleRows },
        buttonsPerRow = { QuickMenu.CONFIG.buttonsPerRow },
        getTotalItems = { getCurrentActions().size }
    ) }
    private val breadcrumbRenderer by lazy { BreadcrumbRenderer(font, menuX, menuY, menuWidth) }
    private val actionButtonGrid by lazy { ActionButtonGrid() }
    private val searchHandler by lazy { SearchHandler { scrollHandler.reset() } }
    private val actionManager by lazy { ActionManager { scrollHandler.reset(); rebuildWidgets() } }
    private val keyEventHandler by lazy { KeyEventHandler(
        onSearchToggle = { isSearching = !isSearching },
        onEditModeToggle = { editMode = !editMode },
        onNavigateBack = { navigateBack() },
        onSearchClear = { searchHandler.clear() },
        onRebuild = { rebuildWidgets() }
    ) }

    override fun init() {
        if (firstInit && !QuickMenu.CONFIG.keepNavigationHistory && NavigationState.depth() > 0) {
            NavigationState.navigateRoot()
        }

        keyEventHandler.updateState(isSearching, editMode)

        if (isSearching) {
            val existingValue = searchHandler.getExistingValue()
            val searchBox = searchHandler.createSearchBox(font, menuX + 8, menuY + 6, menuWidth - 50, 12, existingValue)
            addRenderableWidget(searchBox)
            setInitialFocus(searchBox)
        }

        addToggleButtons()

        val actions = getCurrentActions()

        val startX = menuX + 10
        val startY = menuY + 28

        val buttons = actionButtonGrid.createButtons(
            actions = actions,
            startX = startX,
            startY = startY,
            scrollOffset = scrollHandler.scrollOffset,
            editMode = editMode,
            font = font,
            onLeftClick = { handleLeftClick(it) },
            onRightClick = { handleRightClick(it) }
        )
        buttons.forEach { addRenderableWidget(it) }

        if (editMode) {
            addEditModeButtons()
        }
    }

    private fun addToggleButtons() {
        val toggleButtonsY = menuY + 4
        var currentToggleX = menuX + menuWidth - 22

        if (!QuickMenu.CONFIG.hideEditIcon) {
            addRenderableWidget(Button.builder(Component.literal(if (editMode) "×" else "✎")) {
                editMode = !editMode
                rebuildWidgets()
            }.pos(currentToggleX, toggleButtonsY).size(18, 18).build())
            currentToggleX -= 20
        }

        addRenderableWidget(Button.builder(Component.literal(if (isSearching) "⌫" else "🔍")) {
            isSearching = !isSearching
            if (!isSearching) searchHandler.clear()
            rebuildWidgets()
        }.pos(currentToggleX, toggleButtonsY).size(18, 18).build())
    }

    private fun addEditModeButtons() {
        val editorY = menuY + menuHeight + 8
        addRenderableWidget(Button.builder(Component.literal("+ Action")) {
            openActionEditor(null)
        }.pos(menuX, editorY).size(menuWidth / 2 - 2, 20).build())

        addRenderableWidget(Button.builder(Component.literal("Settings")) {
            minecraft?.setScreen(ModMenuIntegration().getModConfigScreenFactory().create(this))
        }.pos(menuX + menuWidth / 2 + 2, editorY).size(menuWidth / 2 - 2, 20).build())
    }

    private fun getCurrentActions(): List<ActionButtonData> {
        return if (isSearching) {
            searchHandler.getFilteredActions()
        } else {
            NavigationState.getCurrentChildren()
        }
    }

    fun getTotalActionRows(): Int {
        val actions = getCurrentActions()
        return ceil(actions.size.toDouble() / QuickMenu.CONFIG.buttonsPerRow.toDouble()).toInt()
    }

    override fun rebuildWidgets() {
        clearWidgets()
        init()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (scrollHandler.isMouseOverScrollbar(event.x().toDouble(), event.y().toDouble(), menuX + menuWidth - 6, menuY + 28)) {
            scrollHandler.startDragging(event.y().toDouble(), menuY + 28)
            return true
        }

        if (!isSearching) {
            val clickedLevel = breadcrumbRenderer.findClickedBreadcrumb(event.x().toDouble(), event.y().toDouble())
            if (clickedLevel != null) {
                handleBreadcrumbClick(clickedLevel)
                return true
            }
        }

        return super.mouseClicked(event, doubleClick)
    }

    private fun handleBreadcrumbClick(level: Int) {
        if (level == -2) {
            val breadcrumbs = breadcrumbRenderer.getBreadcrumbs()
            val visibleLevels = breadcrumbs.map { it.level }.toSet()
            val omitted = NavigationState.getStackItems().mapIndexedNotNull { index, data ->
                if (!visibleLevels.contains(index)) index to data.second.name else null
            }
            if (omitted.isNotEmpty()) {
                minecraft?.setScreen(BreadcrumbPopupUI(omitted, {
                    navigateToFolderLevel(it)
                }, this))
            }
        } else {
            navigateToFolderLevel(level)
        }
    }

    private fun navigateToFolderLevel(level: Int) {
        NavigationState.navigateToLevel(level)
        scrollHandler.reset()
        rebuildWidgets()
    }

    private fun navigateBack() {
        NavigationState.navigateBack()
        scrollHandler.reset()
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (scrollHandler.isDraggingScrollbar) {
            scrollHandler.updateScrollFromMouse(event.y().toDouble(), menuY + 28)
            rebuildWidgets()
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        scrollHandler.stopDragging()
        if (!editMode && QuickMenu.CONFIG.closeOnKeyReleased) {
            if (ModKeybindings.menuOpenKeybinding.matchesMouse(event)) {
                handleReleaseAction()
                return true
            }
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scrollHandler.scroll(verticalAmount)
        rebuildWidgets()
        return true
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        menuBackground.renderBackground(graphics)

        super.extractRenderState(graphics, mouseX, mouseY, delta)

        val actions = getCurrentActions()
        val totalRows = ceil(actions.size.toDouble() / QuickMenu.CONFIG.buttonsPerRow.toDouble()).toInt()
        menuBackground.renderScrollbar(graphics, totalRows, scrollHandler.scrollOffset, mouseX, mouseY, scrollHandler.isDraggingScrollbar)

        val contentStartY = menuY + 25
        val contentEndY = contentStartY + QuickMenu.CONFIG.visibleRows * 30 + 3
        menuBackground.renderContentFades(graphics, totalRows, scrollHandler.scrollOffset, contentStartY, contentEndY)

        if (!isSearching) {
            breadcrumbRenderer.render(graphics, mouseX, mouseY)
        } else if (searchHandler.isSearchBoxFocused()) {
            menuBackground.renderSearchFocus(graphics)
        }

        if (actions.isEmpty()) {
            menuBackground.renderEmptyMessage(graphics, font)
        }

        if (editMode) {
            val isDeleteDown = keyEventHandler.isKeyMappingDown(ModKeybindings.deleteModifierKeybind)
            val isMoveDown = keyEventHandler.isKeyMappingDown(ModKeybindings.moveModifierKeybind)
            actionButtonGrid.renderEditIndicators(graphics, font, isDeleteDown, isMoveDown)
        }
    }

    private fun handleLeftClick(data: ActionButtonData) {
        if (editMode) {
            when {
                keyEventHandler.isKeyMappingDown(ModKeybindings.deleteModifierKeybind) -> {
                    actionManager.deleteAction(data, isSearching) { minecraft?.setScreen(this) }
                }
                keyEventHandler.isKeyMappingDown(ModKeybindings.moveModifierKeybind) -> {
                    actionManager.moveAction(data, -1, isSearching)
                }
                data.isFolder -> {
                    enterFolder(data)
                }
                else -> {
                    openActionEditor(data)
                }
            }
            return
        }

        if (data.isFolder) {
            enterFolder(data)
        } else {
            data.run()
            if (QuickMenu.CONFIG.closeOnAction) minecraft?.setScreen(null)
        }
    }

    private fun handleRightClick(data: ActionButtonData) {
        if (!editMode) return

        when {
            keyEventHandler.isKeyMappingDown(ModKeybindings.moveModifierKeybind) -> {
                actionManager.moveAction(data, 1, isSearching)
            }
            else -> {
                openActionEditor(data)
            }
        }
    }

    private fun enterFolder(folder: ActionButtonData) {
        if (isSearching) {
            isSearching = false
            searchHandler.clear()
        }
        NavigationState.navigateTo(folder)
        scrollHandler.reset()
        rebuildWidgets()
    }

    private fun openActionEditor(action: ActionButtonData?) {
        val actionEditor = ActionEditorUI(action)
        actionEditor.previousScreen = this
        minecraft?.setScreen(actionEditor)
    }

    private fun handleReleaseAction() {
        val hoveredData = actionButtonGrid.getHoveredData(0.0, 0.0)
        if (hoveredData != null && !hoveredData.isFolder) {
            handleLeftClick(hoveredData)
        }
        minecraft?.setScreen(null)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (keyEventHandler.handleKeyPressed(event)) {
            return true
        }
        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        if (keyEventHandler.handleKeyReleased(event, QuickMenu.CONFIG.closeOnKeyReleased)) {
            handleReleaseAction()
            return true
        }
        return super.keyReleased(event)
    }

    override fun isPauseScreen(): Boolean = false
}