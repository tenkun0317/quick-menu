package xyz.inorganic.quickmenu.ui.surfaces

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu

class MenuBackground(
    var menuX: Int,
    var menuY: Int,
    var menuWidth: Int,
    var menuHeight: Int
) {
    private val rowHeight = 30

    fun renderBackground(graphics: GuiGraphicsExtractor) {
        graphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        renderThinBorder(graphics, menuX, menuY, menuWidth, menuHeight, 0x33FFFFFF.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 24, 0x22FFFFFF.toInt())
        val separatorY = menuY + 24
        graphics.fill(menuX + 1, separatorY, menuX + menuWidth - 1, separatorY + 1, 0x44FFFFFF.toInt())
    }

    fun renderScrollbar(graphics: GuiGraphicsExtractor, totalRows: Int, scrollOffset: Int, mouseX: Int, mouseY: Int, isDragging: Boolean) {
        val config = QuickMenu.CONFIG
        
        if (totalRows > config.visibleRows) {
            val sbX = menuX + menuWidth - 5
            val sbY = menuY + 28
            val sbH = config.visibleRows * rowHeight
            graphics.fill(sbX, sbY, sbX + 3, sbY + sbH, 0x22FFFFFF.toInt())
            val thumbH = maxOf(4, (config.visibleRows.toDouble() / totalRows.toDouble() * sbH).toInt())
            val maxScroll = (totalRows - config.visibleRows) * rowHeight
            val thumbY = if (maxScroll > 0) sbY + (scrollOffset.toDouble() / maxScroll.toDouble() * (sbH - thumbH)).toInt() else sbY
            val thumbColor = if (isDragging || isMouseOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            graphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, thumbColor)
        }
    }

    fun renderContentFades(graphics: GuiGraphicsExtractor, totalRows: Int, scrollOffset: Int, contentStartY: Int, contentEndY: Int) {
        val maxScroll = maxOf(0, (totalRows - QuickMenu.CONFIG.visibleRows) * rowHeight)
        
        if (scrollOffset > 0) {
            graphics.fillGradient(menuX + 1, contentStartY, menuX + menuWidth - 1, contentStartY + 12, 0x99000000.toInt(), 0x00000000.toInt())
        }
        if (scrollOffset < maxScroll) {
            graphics.fillGradient(menuX + 1, contentEndY - 12, menuX + menuWidth - 1, contentEndY, 0x00000000.toInt(), 0x99000000.toInt())
        }
    }

    fun renderSearchFocus(graphics: GuiGraphicsExtractor) {
        graphics.fill(menuX + 8, menuY + 18, menuX + menuWidth - 42, menuY + 19, 0xAAFFFFFF.toInt())
    }

    fun renderEmptyMessage(graphics: GuiGraphicsExtractor, font: net.minecraft.client.gui.Font) {
        val emptyMsg = Component.translatable("menu.main.no_actions")
        val msgW = font.width(emptyMsg)
        graphics.centeredText(font, emptyMsg, menuX + (menuWidth - msgW) / 2, menuY + (menuHeight / 2), 0x66FFFFFF.toInt())
    }

    private fun isMouseOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val sbX = menuX + menuWidth - 6
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= menuY + 28 && mouseY <= menuY + 28 + (QuickMenu.CONFIG.visibleRows * rowHeight)
    }

    private fun renderThinBorder(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y + 1, x + 1, y + h - 1, color)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }
}