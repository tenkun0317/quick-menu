package xyz.inorganic.quickmenu.ui.components

import kotlin.math.ceil

class ScrollState(
    private val rowHeight: Int,
    private val visibleRows: Int,
    private val totalItemsProvider: () -> Int
) {
    var scrollOffset = 0
    var isDraggingScrollbar = false

    private val totalHeight: Int
        get() = ceil(totalItemsProvider().toDouble() / 1.0).toInt() * rowHeight

    private val visibleHeight: Int
        get() = visibleRows * rowHeight

    val maxScroll: Int
        get() = maxOf(0, totalHeight - visibleHeight)

    fun isMouseOverScrollbar(mouseX: Double, mouseY: Double, scrollbarX: Int, scrollbarY: Int, scrollbarWidth: Int = 4): Boolean {
        val total = totalItemsProvider()
        val totalR = ceil(total.toDouble() / 1.0).toInt()
        if (totalR <= visibleRows) return false
        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= scrollbarY && mouseY <= scrollbarY + visibleHeight
    }

    fun updateScrollFromMouse(mouseY: Double, scrollbarY: Int) {
        val percentage = ((mouseY - scrollbarY) / visibleHeight.toDouble()).coerceIn(0.0, 1.0)
        scrollOffset = ((percentage * maxScroll).toInt() / rowHeight) * rowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
    }

    fun scroll(verticalAmount: Double) {
        scrollOffset = (scrollOffset - (verticalAmount * rowHeight).toInt()).coerceIn(0, maxScroll)
    }

    fun getScrollbarThumbHeight(totalItems: Int, buttonsPerRow: Int = 1): Int {
        val totalRows = ceil(totalItems.toDouble() / buttonsPerRow.toDouble()).toInt()
        if (totalRows <= visibleRows) return 4
        return maxOf(4, (visibleRows.toDouble() / totalRows.toDouble() * visibleHeight).toInt())
    }

    fun getScrollbarThumbY(totalItems: Int, scrollbarY: Int, buttonsPerRow: Int = 1): Int {
        val totalRows = ceil(totalItems.toDouble() / buttonsPerRow.toDouble()).toInt()
        val maxScroll = maxOf(0, (totalRows - visibleRows) * rowHeight)
        val thumbH = getScrollbarThumbHeight(totalItems, buttonsPerRow)
        return if (maxScroll > 0) scrollbarY + (scrollOffset.toDouble() / maxScroll.toDouble() * (visibleHeight - thumbH)).toInt() else scrollbarY
    }
}