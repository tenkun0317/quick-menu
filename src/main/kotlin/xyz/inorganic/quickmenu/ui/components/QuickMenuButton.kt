package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
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
    var isFolder: Boolean = false
) : Button(0, 0, 26, 26, Component.empty(), onPress, DEFAULT_NARRATION) {

    private val FOLDER_ICON = Identifier.parse("quickmenu:textures/folder_icon.png")

    override fun renderContents(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // --- Modern Flat Style ---
        val alpha = if (!active) 0x22 else if (isHovered) 0x66 else 0x44
        val color = (alpha shl 24) or 0xFFFFFF
        
        guiGraphics.fill(x, y, x + width, y + height, color)
        
        val borderColor = if (isHovered && active) 0xAAFFFFFF.toInt() else 0x22FFFFFF.toInt()
        renderOutline(guiGraphics, x, y, width, height, borderColor)

        if (!itemIcon.isEmpty) {
            guiGraphics.renderItem(itemIcon, x + (width - 16) / 2, y + (height - 16) / 2)
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemIcon, x + (width - 16) / 2, y + (height - 16) / 2)
        }

        // --- Folder Indicator ---
        if (isFolder) {
            // Draw a small folder-like icon in the top-left
            val fx = x + 2
            val fy = y + 2
            val fColor = 0xFFFFAA00.toInt()
            
            // Folder body
            guiGraphics.fill(fx, fy + 2, fx + 8, fy + 7, fColor)
            // Folder tab
            guiGraphics.fill(fx, fy + 1, fx + 3, fy + 2, fColor)
            
            // Outline for better visibility on bright icons
            renderOutline(guiGraphics, fx - 1, fy, 10, 8, 0x88000000.toInt())
        }
    }

    private fun renderOutline(guiGraphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        guiGraphics.fill(x, y, x + w, y + 1, color)
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color)
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, color)
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
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
