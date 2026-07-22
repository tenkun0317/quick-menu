package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.ModConfig
import kotlin.math.*

class RadialMenuUI : Screen(Component.translatable("menu.radial.title")) {
    private var centerX = 0
    private var centerY = 0
    private var currentPage = 0
    private var hoveredAction: ActionButtonData? = null
    private var hoveredSlotIndex = -1

    companion object {
        private var lastPage = 0
    }

    override fun init() {
        centerX = width / 2
        centerY = height / 2

        if (lastPage < totalPages()) {
            currentPage = lastPage
        } else {
            currentPage = 0
        }
    }

    private fun maxItemsPerPage(): Int {
        return QuickMenu.CONFIG.radialMaxItems.coerceIn(1, 16)
    }

    private fun collectRadialItems(items: List<ActionButtonData>, onlyRegistered: Boolean): List<ActionButtonData> {
        val result = mutableListOf<ActionButtonData>()
        for (item in items) {
            if (!item.isFolder) {
                if (!onlyRegistered || item.registeredForRadial) {
                    result.add(item)
                }
            } else {
                result.addAll(collectRadialItems(item.children, onlyRegistered))
            }
        }
        return result
    }

    private fun getRadialItems(): List<ActionButtonData> {
        val config = QuickMenu.CONFIG
        return if (config.radialDisplayMode == ModConfig.RadialDisplayMode.DYNAMIC) {
            collectRadialItems(ActionButtonDataHandler.actions, true)
        } else {
            collectRadialItems(ActionButtonDataHandler.actions, false)
        }
    }

    private fun getCurrentPageItems(): List<ActionButtonData> {
        val items = getRadialItems()
        val start = currentPage * maxItemsPerPage()
        val end = min(start + maxItemsPerPage(), items.size)
        return if (start < items.size) items.subList(start, end) else emptyList()
    }

    private fun totalPages(): Int {
        val items = getRadialItems()
        return maxOf(1, (items.size + maxItemsPerPage() - 1) / maxItemsPerPage())
    }

    private fun getSlotCount(): Int {
        return if (QuickMenu.CONFIG.radialDisplayMode == ModConfig.RadialDisplayMode.STATIC) {
            maxItemsPerPage()
        } else {
            getCurrentPageItems().size
        }
    }

    private fun getRadius(): Float {
        return QuickMenu.CONFIG.radialRadius.toFloat()
    }

    private fun getDeadZoneRadius(): Float {
        return QuickMenu.CONFIG.radialDeadZoneRadius.toFloat()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val total = totalPages()
        if (total <= 1) return false
        currentPage = ((currentPage - verticalAmount.toInt()) % total + total) % total
        lastPage = currentPage
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        updateHovered(mouseX, mouseY)
    }

    private fun updateHovered(mouseX: Double, mouseY: Double) {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        val dist = sqrt(dx * dx + dy * dy).toFloat()

        if (dist < getDeadZoneRadius()) {
            hoveredAction = null
            return
        }

        val items = getCurrentPageItems()
        val slots = getSlotCount()
        if (slots == 0) {
            hoveredAction = null
            return
        }

        val sliceAngle = 2 * PI / slots
        val mouseAngle = atan2(dy, dx)
        val item0Center = -PI / 2 - sliceAngle / 2
        val shifted = (mouseAngle - item0Center + sliceAngle / 2 + 2 * PI) % (2 * PI)
        val slotIndex = (shifted / sliceAngle).toInt()

        hoveredSlotIndex = slotIndex
        hoveredAction = if (slotIndex in items.indices) items[slotIndex] else null
    }

    private fun isInDeadZone(mouseX: Double, mouseY: Double): Boolean {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        return sqrt(dx * dx + dy * dy) <= getDeadZoneRadius()
    }

    fun handleKeyRelease() {
        val client = minecraft
        val action = hoveredAction
        if (action != null && !action.isFolder) {
            action.run()
            if (QuickMenu.CONFIG.radialCloseOnAction) {
                client.gui.setScreen(null)
                return
            }
        }
        client.gui.setScreen(null)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        updateHovered(mouseX.toDouble(), mouseY.toDouble())

        graphics.fill(0, 0, width, height, 0xAA000000.toInt())

        val items = getCurrentPageItems()
        val slots = getSlotCount()
        if (slots == 0) {
            val emptyMsg = Component.translatable("menu.main.no_actions")
            graphics.centeredText(font, emptyMsg, centerX, centerY, 0x66FFFFFF.toInt())
            return
        }

        val radius = getRadius()
        val deadZone = getDeadZoneRadius()
        val sliceAngle = (2 * PI / slots).toFloat()
        val startAngle = (-PI / 2 - sliceAngle / 2).toFloat()

        for (slot in 0 until slots) {
            val angle = (startAngle + slot * sliceAngle).toDouble()
            val itemX = (centerX + radius * cos(angle)).toInt()
            val itemY = (centerY + radius * sin(angle)).toInt()
            val hasItem = slot < items.size
            val item = if (hasItem) items[slot] else null
            val isHovered = slot == hoveredSlotIndex

            val bgColor = if (isHovered) 0x66FFFFFF.toInt() else if (hasItem) 0x44FFFFFF.toInt() else 0x22FFFFFF.toInt()
            val borderColor = if (isHovered) 0xAAFFFFFF.toInt() else if (hasItem) 0x22FFFFFF.toInt() else 0x11FFFFFF.toInt()

            graphics.fill(itemX - 14, itemY - 14, itemX + 14, itemY + 14, bgColor)
            drawThinBorder(graphics, itemX - 14, itemY - 14, 28, 28, borderColor)

            if (hasItem && !item!!.icon.isEmpty) {
                graphics.item(item.icon, itemX - 8, itemY - 8)
            }

            val displayName = if (hasItem) {
                if (item!!.name.length > 8) item.name.take(7) + "..." else item.name
            } else ""
            if (displayName.isNotEmpty()) {
                val textWidth = font.width(displayName)
                graphics.text(font, displayName, itemX - textWidth / 2, itemY + 16, 0xFFCCCCCC.toInt(), true)
            }
        }

        val deadColor = if (isInDeadZone(mouseX.toDouble(), mouseY.toDouble())) 0x33FFFFFF.toInt() else 0x22FFFFFF.toInt()
        graphics.fill(centerX - deadZone.toInt(), centerY - deadZone.toInt(), centerX + deadZone.toInt(), centerY + deadZone.toInt(), deadColor)
        drawThinBorder(graphics, centerX - deadZone.toInt(), centerY - deadZone.toInt(), (deadZone * 2).toInt(), (deadZone * 2).toInt(), 0x44FFFFFF.toInt())

        val total = totalPages()
        if (total > 1) {
            val pageText = "Page ${currentPage + 1} / $total"
            val pageWidth = font.width(pageText)
            graphics.text(font, pageText, centerX - pageWidth / 2, centerY - radius.toInt() - 30, 0xFFAAAAAA.toInt(), true)
        }

        if (hoveredAction != null) {
            val name = hoveredAction!!.name
            val nameWidth = font.width(name)
            graphics.text(font, name, centerX - nameWidth / 2, centerY + radius.toInt() + 20, -1, true)
        }
    }

    private fun drawThinBorder(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y + 1, x + 1, y + h - 1, color)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }

    override fun isPauseScreen(): Boolean = false
}
