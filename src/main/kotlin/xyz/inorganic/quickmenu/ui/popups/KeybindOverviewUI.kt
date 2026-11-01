package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.ActionKeyCodes
import xyz.inorganic.quickmenu.other.KeybindConflicts
import xyz.inorganic.quickmenu.ui.ActionEditorUI
import xyz.inorganic.quickmenu.ui.components.MenuScrollHandler

class KeybindOverviewUI : Screen(Component.translatable("menu.keybind_overview.title")) {
    var previousScreen: Screen? = null

    private data class Row(val data: ActionButtonData, val path: String, val keybindLabel: String, val conflicting: Boolean)

    private var rows: List<Row> = emptyList()

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 0
    private var menuHeight = 0

    private var listTop = 0
    private var listBottom = 0
    private var listLeft = 0
    private var entryRight = 0

    private val scrollHandler = MenuScrollHandler(
        rowHeight = ROW_HEIGHT,
        getTotalHeight = { rows.size * ROW_HEIGHT },
        getVisibleHeight = { listBottom - listTop }
    )

    companion object {
        private const val ROW_HEIGHT = 20
    }

    override fun init() {
        menuWidth = minOf(340, width - 16)
        menuHeight = minOf(320, height - 16)
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        listTop = menuY + 26
        listBottom = menuY + menuHeight - 10
        listLeft = menuX + 10
        entryRight = menuX + menuWidth - 12

        rebuildRows()
    }

    private fun rebuildRows() {
        val conflicts = KeybindConflicts.conflictingKeybinds()
        rows = KeybindConflicts.collectEntries().map { entry ->
            Row(entry.data, entry.path, describeKeybind(entry.data), conflicts.contains(entry.data.keybind.toList()))
        }
    }

    private fun describeKeybind(data: ActionButtonData): String {
        val keybind = data.keybind
        if (keybind.size < 4) return ""
        return if (keybind[3] != 0) {
            Component.translatable("text.quickmenu.mouse_button", keybind[0] + 1).string
        } else {
            val mods = keybind[2]
            val sb = StringBuilder()
            if (mods and 2 != 0) sb.append("Ctrl+")
            if (mods and 1 != 0) sb.append("Shift+")
            if (mods and 4 != 0) sb.append("Alt+")
            sb.append(
                InputConstants.Type.KEYSYM
                    .getOrCreate(ActionKeyCodes.toNativeKey(keybind[0]))
                    .displayName.string
            )
            sb.toString()
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scrollHandler.scroll(verticalAmount)
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x()
        val my = event.y()
        if (mx >= listLeft && mx <= entryRight && my >= listTop && my <= listBottom) {
            val index = ((my - listTop).toInt() + scrollHandler.scrollOffset) / ROW_HEIGHT
            if (index in rows.indices) {
                val editor = ActionEditorUI(rows[index].data)
                editor.previousScreen = this
                minecraft?.gui?.setScreen(editor)
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (scrollHandler.isDraggingScrollbar) {
            scrollHandler.updateScrollFromMouse(dragY, menuX + menuWidth - 8, listBottom - listTop)
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        scrollHandler.stopDragging()
        return super.mouseReleased(event)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x55000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC000000.toInt())
        drawThinBorder(graphics, menuX, menuY, menuWidth, menuHeight, 0xFF555555.toInt())

        val title = Component.translatable("menu.keybind_overview.title")
        graphics.text(font, title, listLeft, menuY + 8, 0xFFFFFFFF.toInt(), true)

        super.extractRenderState(graphics, mouseX, mouseY, delta)

        val scrollbarX = menuX + menuWidth - 8
        graphics.enableScissor(listLeft, listTop, entryRight, listBottom)
        var y = listTop - scrollHandler.scrollOffset
        for (row in rows) {
            if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                val hovered = mouseX >= listLeft && mouseX <= entryRight && mouseY >= y && mouseY < y + ROW_HEIGHT
                val bg = if (hovered) 0x44FFFFFF.toInt() else 0x22FFFFFF.toInt()
                graphics.fill(listLeft, y + 1, entryRight, y + ROW_HEIGHT - 1, bg)

                val nameColor = if (row.conflicting) 0xFFFF5555.toInt() else 0xFFE0E0E0.toInt()
                graphics.text(font, row.path, listLeft + 2, y + 6, nameColor, true)

                val keyWidth = font.width(row.keybindLabel)
                val keyColor = if (row.conflicting) 0xFFFF5555.toInt() else 0xFF888888.toInt()
                graphics.text(font, row.keybindLabel, entryRight - 4 - keyWidth, y + 6, keyColor, true)
            }
            y += ROW_HEIGHT
        }
        graphics.disableScissor()

        if (scrollHandler.hasScrollbar()) {
            val sbH = listBottom - listTop
            graphics.fill(scrollbarX, listTop, scrollbarX + 4, listBottom, 0x22888888.toInt())
            val thumbH = scrollHandler.thumbHeight(sbH)
            val thumbY = scrollHandler.thumbY(listTop, sbH)
            val thumbColor = if (scrollHandler.isDraggingScrollbar) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, thumbColor)
        }
    }

    private fun drawThinBorder(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y + 1, x + 1, y + h - 1, color)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
