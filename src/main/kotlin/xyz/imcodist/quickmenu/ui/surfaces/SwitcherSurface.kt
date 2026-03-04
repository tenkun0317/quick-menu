package xyz.imcodist.quickmenu.ui.surfaces

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier
import kotlin.math.ceil

object SwitcherSurface {
    private val TEXTURE = Identifier.parse("quickmenu:textures/switcher_textures.png")

    fun draw(context: GuiGraphics, x: Int, y: Int, width: Int, height: Int, isHeader: Boolean = false) {
        val sourceX = if (isHeader) 0 else 24
        drawNineSlicedTexture(context, x, y, width, height, sourceX, 0, 6, 6, 12, 12, 52, 50)
    }

    private fun drawNineSlicedTexture(
        context: GuiGraphics, x: Int, y: Int, width: Int, height: Int,
        sourceX: Int, sourceY: Int, sideWidth: Int, sideHeight: Int,
        centerWidth: Int, centerHeight: Int, textureWidth: Int, textureHeight: Int
    ) {
        drawRepeatingTexture(context, x + sideWidth, y, sourceX + sideWidth, sourceY, centerWidth, sideHeight, textureWidth, textureHeight, width - (sideWidth * 2), sideHeight)
        drawRepeatingTexture(context, x + sideWidth, y + height - sideHeight, sourceX + sideWidth, sourceY + sideHeight + centerHeight, centerWidth, sideHeight, textureWidth, textureHeight, width - (sideWidth * 2), sideHeight)
        drawRepeatingTexture(context, x, y + sideHeight, sourceX, sourceY + sideHeight, sideWidth, centerHeight, textureWidth, textureHeight, sideWidth, height - (sideHeight * 2))
        drawRepeatingTexture(context, x + width - sideWidth, y + sideHeight, sourceX + sideWidth + centerWidth, sourceY + sideHeight, sideWidth, centerHeight, textureWidth, textureHeight, sideWidth, height - (sideHeight * 2))
        drawTexture(context, x, y, sourceX, sourceY, sideWidth, sideHeight, textureWidth, textureHeight)
        drawTexture(context, x + width - sideWidth, y, sourceX + sideWidth + centerWidth, sourceY, sideWidth, sideHeight, textureWidth, textureHeight)
        drawTexture(context, x, y + height - sideHeight, sourceX, sourceY + sideHeight + centerHeight, sideWidth, sideHeight, textureWidth, textureHeight)
        drawTexture(context, x + width - sideWidth, y + height - sideHeight, sourceX + sideWidth + centerWidth, sourceY + sideHeight + centerHeight, sideWidth, sideHeight, textureWidth, textureHeight)
        drawRepeatingTexture(context, x + sideWidth, y + sideHeight, sourceX + sideWidth, sourceY + sideHeight, centerWidth, centerHeight, textureWidth, textureHeight, width - (sideWidth * 2), height - (sideHeight * 2))
    }

    private fun drawTexture(
        context: GuiGraphics, x: Int, y: Int, sourceX: Int, sourceY: Int,
        sourceWidth: Int, sourceHeight: Int, textureWidth: Int, textureHeight: Int
    ) {
        context.blit(TEXTURE, x, y, sourceX, sourceY, sourceWidth.toFloat(), sourceHeight.toFloat(), textureWidth.toFloat(), textureHeight.toFloat())
    }

    private fun drawRepeatingTexture(
        context: GuiGraphics, x: Int, y: Int, sourceX: Int, sourceY: Int,
        sourceWidth: Int, sourceHeight: Int, textureWidth: Int, textureHeight: Int, width: Int, height: Int
    ) {
        if (width <= 0 || height <= 0) return
        val xMax = width.toDouble() / sourceWidth.toDouble()
        val yMax = height.toDouble() / sourceHeight.toDouble()

        for (xi in 0 until ceil(xMax).toInt()) {
            for (yi in 0 until ceil(yMax).toInt()) {
                var newWidth = sourceWidth
                var newHeight = sourceHeight
                if (xi == ceil(xMax).toInt() - 1) newWidth = width - (sourceWidth * xi)
                if (yi == ceil(yMax).toInt() - 1) newHeight = height - (sourceHeight * yi)

                if (newWidth > 0 && newHeight > 0) {
                    drawTexture(context, x + (sourceWidth * xi), y + (sourceHeight * yi), sourceX, sourceY, newWidth, newHeight, textureWidth, textureHeight)
                }
            }
        }
    }
}
