package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton

class ActionButtonGrid {
    private val buttonDataMap = mutableMapOf<QuickMenuButton, ActionButtonData>()
    private val rowHeight = 30

    fun createButtons(
        actions: List<ActionButtonData>,
        startX: Int,
        startY: Int,
        scrollOffset: Int,
        editMode: Boolean,
        font: Font,
        onLeftClick: (ActionButtonData) -> Unit,
        onRightClick: (ActionButtonData) -> Unit
    ): List<QuickMenuButton> {
        val config = QuickMenu.CONFIG
        buttonDataMap.clear()
        val buttons = mutableListOf<QuickMenuButton>()

        val visibleAreaHeight = config.visibleRows * rowHeight

        actions.forEachIndexed { index, data ->
            val row = index / config.buttonsPerRow
            val col = index % config.buttonsPerRow
            val btnX = startX + col * 30
            val btnY = startY + row * rowHeight - scrollOffset

            if (btnY >= startY && btnY + 26 <= startY + visibleAreaHeight) {
                val button = QuickMenuButton(data.icon, { onLeftClick(data) }, { onRightClick(data) }, data.isFolder, data.registeredForRadial)
                button.x = btnX
                button.y = btnY
                button.setTooltip(Tooltip.create(Component.literal(data.name)))
                buttons.add(button)
                buttonDataMap[button] = data
            }
        }
        return buttons
    }

    fun getHoveredData(mouseX: Double, mouseY: Double): ActionButtonData? {
        val hoveredBtn = buttonDataMap.keys.find { it.isHovered }
        return hoveredBtn?.let { buttonDataMap[it] }
    }

    fun renderEditIndicators(
        graphics: net.minecraft.client.gui.GuiGraphicsExtractor,
        font: Font,
        isDeleteDown: Boolean,
        isMoveDown: Boolean
    ) {
        buttonDataMap.keys.forEach { btn ->
            if (btn.isHovered) {
                if (isDeleteDown) renderIndicator(graphics, btn, font, 0xFFFF0000.toInt(), "×")
                else if (isMoveDown) renderIndicator(graphics, btn, font, 0xFF00AAFF.toInt(), "↔")
            }
        }
    }

    private fun renderIndicator(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, btn: QuickMenuButton, font: Font, color: Int, text: String) {
        val xSize = 10
        val xX = btn.x + btn.width - xSize + 2
        val xY = btn.y - 2
        graphics.fill(xX, xY, xX + xSize, xY + xSize, color)
        graphics.text(font, text, xX + (xSize - font.width(text)) / 2 + 1, xY + 1, -1, false)
    }

    fun clear() = buttonDataMap.clear()
}