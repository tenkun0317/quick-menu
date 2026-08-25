package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.other.KeybindHandler
import java.util.function.Consumer

class KeybindPickerUI : Screen(Component.empty()) {
    var onSelectedKeybind: Consumer<KeyMapping> = Consumer {}
    var previousScreen: Screen? = null

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 0
    private var menuHeight = 0

    private lateinit var searchBox: EditBox
    private var searchText = ""

    private sealed interface Row {
        data class Header(val category: KeyMapping.Category) : Row
        data class Entry(val keyMapping: KeyMapping) : Row
    }

    private var rows: List<Row> = emptyList()
    private var scrollOffset = 0

    private var listTop = 0
    private var listBottom = 0
    private var listLeft = 0
    private var entryRight = 0
    private var scrollbarX = 0
    private var thumbHeight = 0
    private var thumbY = 0
    private var draggingScrollbar = false
    private var dragStartMouseY = 0.0
    private var dragStartScroll = 0

    companion object {
        private const val ROW_HEIGHT = 22
        private const val SCROLLBAR_WIDTH = 6
    }

    override fun init() {
        menuWidth = minOf(380, width - 16)
        menuHeight = minOf(440, height - 16)
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        listTop = menuY + 36
        listBottom = menuY + menuHeight - 10
        listLeft = menuX + 10
        scrollbarX = menuX + menuWidth - 10 - SCROLLBAR_WIDTH
        entryRight = scrollbarX - 4

        clearWidgets()
        searchBox = EditBox(font, listLeft, menuY + 10, entryRight - listLeft, 16, Component.empty())
        searchBox.setMaxLength(60)
        searchBox.setValue(searchText)
        searchBox.setHint(Component.translatable("menu.keybind_picker.search"))
        searchBox.setResponder {
            searchText = it
            rebuildRows()
        }
        addRenderableWidget(searchBox)
        setFocused(searchBox)

        rebuildRows()
    }

    private fun listHeight(): Int = listBottom - listTop

    private fun maxScroll(): Int = maxOf(0, rows.size * ROW_HEIGHT - listHeight())

    private fun rebuildRows() {
        val bindings = KeybindHandler.getKeybindings() ?: emptyArray()
        val query = searchText.trim().lowercase()

        rows = buildList {
            if (query.isEmpty()) {
                var lastCategory: KeyMapping.Category? = null
                for (binding in bindings) {
                    if (binding.category != lastCategory) {
                        add(Row.Header(binding.category))
                        lastCategory = binding.category
                    }
                    add(Row.Entry(binding))
                }
            } else {
                for (binding in bindings) {
                    val label = binding.translatedKeyMessage.string
                    val name = Component.translatable(binding.name).string
                    if (name.lowercase().contains(query) || label.lowercase().contains(query)) {
                        add(Row.Entry(binding))
                    }
                }
            }
        }

        scrollOffset = scrollOffset.coerceIn(0, maxScroll())
        updateThumb()
    }

    private fun updateThumb() {
        val maxScroll = maxScroll()
        thumbHeight = if (maxScroll <= 0 || rows.isEmpty()) {
            listHeight()
        } else {
            maxOf(24, listHeight() * listHeight() / (rows.size * ROW_HEIGHT))
        }
        val travel = listHeight() - thumbHeight
        thumbY = if (maxScroll <= 0 || travel <= 0) {
            listTop
        } else {
            listTop + travel * scrollOffset / maxScroll
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val maxScroll = maxScroll()
        scrollOffset = (scrollOffset - (verticalAmount * ROW_HEIGHT).toInt()).coerceIn(0, maxScroll)
        updateThumb()
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (super.mouseClicked(event, doubled)) return true
        val mx = event.x()
        val my = event.y()

        if (maxScroll() > 0 && mx >= scrollbarX && mx <= scrollbarX + SCROLLBAR_WIDTH &&
            my >= listTop && my <= listBottom
        ) {
            if (my >= thumbY && my <= thumbY + thumbHeight) {
                draggingScrollbar = true
                dragStartMouseY = my
                dragStartScroll = scrollOffset
            } else {
                val ratio = (my - listTop - thumbHeight / 2.0) / (listHeight() - thumbHeight).coerceAtLeast(1)
                scrollOffset = (ratio * maxScroll()).toInt().coerceIn(0, maxScroll())
                updateThumb()
                draggingScrollbar = true
                dragStartMouseY = my
                dragStartScroll = scrollOffset
            }
            return true
        }

        if (mx >= listLeft && mx <= entryRight && my >= listTop && my <= listBottom) {
            val index = (my - listTop + scrollOffset).toInt() / ROW_HEIGHT
            if (index in rows.indices) {
                val row = rows[index]
                if (row is Row.Entry) {
                    onSelectedKeybind.accept(row.keyMapping)
                    onClose()
                    return true
                }
            }
        }
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (draggingScrollbar) {
            val maxScroll = maxScroll()
            val travel = (listHeight() - thumbHeight).coerceAtLeast(1)
            val delta = (event.y() - dragStartMouseY) / travel * maxScroll
            scrollOffset = (dragStartScroll + delta).toInt().coerceIn(0, maxScroll)
            updateThumb()
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingScrollbar = false
        return super.mouseReleased(event)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x55000000)
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC000000.toInt())
        drawThinBorder(graphics, menuX, menuY, menuWidth, menuHeight, 0xFF555555.toInt())

        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.enableScissor(listLeft, listTop, entryRight, listBottom)
        var y = listTop - scrollOffset
        for (row in rows) {
            if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                when (row) {
                    is Row.Header -> drawHeader(graphics, row.category, y)
                    is Row.Entry -> drawEntry(graphics, row.keyMapping, y, mouseX, mouseY)
                }
            }
            y += ROW_HEIGHT
        }
        graphics.disableScissor()

        if (maxScroll() > 0) {
            graphics.fill(scrollbarX, listTop, scrollbarX + SCROLLBAR_WIDTH, listBottom, 0x22888888.toInt())
            val hoveredThumb = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight
            val thumbColor = if (draggingScrollbar || hoveredThumb) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, thumbColor)
        }
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, category: KeyMapping.Category, y: Int) {
        val label = category.label()
        graphics.text(font, label, listLeft + 2, y + (ROW_HEIGHT - 9) / 2, 0xFF999999.toInt(), true)
        graphics.fill(listLeft + font.width(label) + 6, y + ROW_HEIGHT / 2, entryRight, y + ROW_HEIGHT / 2 + 1, 0x33888888.toInt())
    }

    private fun drawEntry(graphics: GuiGraphicsExtractor, keyMapping: KeyMapping, y: Int, mouseX: Int, mouseY: Int) {
        val hovered = mouseX >= listLeft && mouseX <= entryRight &&
            mouseY >= y && mouseY < y + ROW_HEIGHT && mouseY >= listTop && mouseY <= listBottom
        val bgColor = if (hovered) 0x44FFFFFF.toInt() else 0x22FFFFFF.toInt()
        graphics.fill(listLeft, y + 1, entryRight, y + ROW_HEIGHT - 1, bgColor)

        val label = Component.translatable(keyMapping.name)
        val textY = y + (ROW_HEIGHT - 9) / 2
        graphics.text(font, label, listLeft + 6, textY, 0xFFE0E0E0.toInt(), true)

        val keyName = keyMapping.translatedKeyMessage
        val keyWidth = font.width(keyName)
        graphics.text(font, keyName, entryRight - 6 - keyWidth, textY, if (hovered) 0xFFFFFFFF.toInt() else 0xFF888888.toInt(), true)
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
