package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor

object UIRenderer {

    fun drawBorder(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, color: Int) {
        graphics.fill(x, y, x + width, y + 1, color)
        graphics.fill(x, y + height - 1, x + width, y + height, color)
        graphics.fill(x, y + 1, x + 1, y + height - 1, color)
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }

    fun drawScrollbar(
        graphics: GuiGraphicsExtractor,
        scrollbarX: Int,
        scrollbarY: Int,
        scrollbarHeight: Int,
        thumbY: Int,
        thumbHeight: Int,
        isActive: Boolean
    ) {
        val thumbColor = if (isActive) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
        graphics.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, thumbColor)
    }

    fun drawGradientFade(graphics: GuiGraphicsExtractor, x: Int, startY: Int, endY: Int, width: Int, topFade: Boolean, bottomFade: Boolean) {
        if (topFade) {
            graphics.fillGradient(x + 1, startY, x + width - 1, startY + 12, 0x99000000.toInt(), 0x00000000.toInt())
        }
        if (bottomFade) {
            graphics.fillGradient(x + 1, endY - 12, x + width - 1, endY, 0x00000000.toInt(), 0x99000000.toInt())
        }
    }
}