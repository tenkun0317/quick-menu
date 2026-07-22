package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer

class QuickMenuButton(
    var itemIcon: ItemStack = ItemStack.EMPTY,
    onPress: OnPress,
    var onRightClick: Consumer<QuickMenuButton> = Consumer {},
    var isFolder: Boolean = false,
    var registeredForRadial: Boolean = false
) : Button(0, 0, 26, 26, Component.empty(), onPress, DEFAULT_NARRATION) {

    private val FOLDER_ICON = Identifier.fromNamespaceAndPath("quickmenu", "textures/folder_icon.png")

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val alpha = if (!active) 0x22 else if (isHovered) 0x66 else 0x44
        val color = (alpha shl 24) or 0xFFFFFF

        graphics.fill(x, y, x + width, y + height, color)

        val borderColor = if (isHovered && active) 0xAAFFFFFF.toInt() else 0x22FFFFFF.toInt()
        renderOutline(graphics, x, y, width, height, borderColor)

        if (!itemIcon.isEmpty) {
            graphics.item(itemIcon, x + (width - 16) / 2, y + (height - 16) / 2)
            graphics.itemDecorations(Minecraft.getInstance().font, itemIcon, x + (width - 16) / 2, y + (height - 16) / 2)
        }

        if (isFolder) {
            val fx = x + 2
            val fy = y + 2
            val fColor = 0xFFFFAA00.toInt()

            graphics.fill(fx, fy + 2, fx + 8, fy + 7, fColor)
            graphics.fill(fx, fy + 1, fx + 3, fy + 2, fColor)

            renderOutline(graphics, fx - 1, fy, 10, 8, 0x88000000.toInt())
        }

        if (registeredForRadial) {
            val rx = x + width - 9
            val ry = y + height - 9
            val rColor = 0xFF00FFAA.toInt()
            graphics.fill(rx, ry, rx + 8, ry + 8, rColor)
            graphics.fill(rx + 2, ry + 1, rx + 6, ry + 3, 0xFF000000.toInt())
            graphics.fill(rx + 1, ry + 3, rx + 3, ry + 7, 0xFF000000.toInt())
            graphics.fill(rx + 5, ry + 3, rx + 7, ry + 7, 0xFF000000.toInt())
        }
    }

    private fun renderOutline(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y + 1, x + 1, y + h - 1, color)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (active && visible && isMouseOver(event.x(), event.y()) && event.button() == GLFW.GLFW_MOUSE_BUTTON_2) {
            playDownSound(Minecraft.getInstance().soundManager)
            onRightClick.accept(this)
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }
}