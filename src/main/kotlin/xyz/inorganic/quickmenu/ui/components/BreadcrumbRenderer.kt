package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.ui.NavigationState

class BreadcrumbRenderer(
    private val font: Font,
    private val menuX: Int,
    private val menuY: Int,
    private val menuWidth: Int
) {
    data class BreadcrumbItem(val label: String, val level: Int, val bounds: Pair<Int, Int>)

    fun getBreadcrumbs(): List<BreadcrumbItem> {
        val maxWidth = menuWidth - 30
        var currentX = menuX + 10

        val rootWidth = font.width("Root")
        var totalWidth = currentX + rootWidth + 5

        val allItems = NavigationState.getStackItems().map { (index, data) ->
            val label = data.name
            val w = font.width("> $label")
            val itemWidth = w + 5
            totalWidth += itemWidth
            Triple(label, index, w)
        }

        if (totalWidth <= menuX + maxWidth) {
            val result = mutableListOf<BreadcrumbItem>()
            var x = currentX
            result.add(BreadcrumbItem("Root", -1, x to (x + rootWidth)))
            x += rootWidth + 5
            allItems.forEach { (label, index, w) ->
                result.add(BreadcrumbItem(label, index, x to (x + w)))
                x += w + 5
            }
            return result
        }

        val result = mutableListOf<BreadcrumbItem>()
        var x = currentX

        result.add(BreadcrumbItem("Root", -1, x to (x + rootWidth)))
        x += rootWidth + 5

        val dotsW = font.width("> ...")
        result.add(BreadcrumbItem("...", -2, x to (x + dotsW)))
        x += dotsW + 5

        val availableWidth = (menuX + maxWidth) - x
        val trailingItems = mutableListOf<BreadcrumbItem>()
        var usedTrailingWidth = 0

        for (i in allItems.indices.reversed()) {
            val (label, index, w) = allItems[i]
            if (usedTrailingWidth + w + 5 <= availableWidth) {
                trailingItems.add(0, BreadcrumbItem(label, index, 0 to 0))
                usedTrailingWidth += w + 5
            } else {
                break
            }
        }

        if (trailingItems.isEmpty() && allItems.isNotEmpty()) {
            val (label, index, w) = allItems.last()
            trailingItems.add(BreadcrumbItem(label, index, 0 to 0))
        }

        trailingItems.forEach { item ->
            val w = font.width("> ${item.label}")
            val finalItem = item.copy(bounds = x to (x + w))
            result.add(finalItem)
            x += w + 5
        }

        return result
    }

    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val y = menuY + 8
        val breadcrumbs = getBreadcrumbs()

        breadcrumbs.forEach { (label, level, bounds) ->
            val isRoot = level == -1
            val isDots = level == -2
            val displayText = if (isRoot) label else "> $label"

            val isHovered = mouseX >= bounds.first && mouseX <= bounds.second && mouseY >= y && mouseY <= y + 9
            val isLast = level == NavigationState.depth() - 1

            val color = when {
                isDots -> 0xFF666666.toInt()
                isLast -> -1
                isHovered -> 0xFFFFFFFF.toInt()
                else -> 0xFFAAAAAA.toInt()
            }

            graphics.text(font, displayText, bounds.first, y, color, true)
        }
    }

    fun findClickedBreadcrumb(mouseX: Double, mouseY: Double): Int? {
        val y = menuY + 8
        val breadcrumbs = getBreadcrumbs()

        breadcrumbs.forEach { (label, level, bounds) ->
            if (mouseX >= bounds.first && mouseX <= bounds.second && mouseY >= y && mouseY <= y + 9) {
                return level
            }
        }
        return null
    }
}