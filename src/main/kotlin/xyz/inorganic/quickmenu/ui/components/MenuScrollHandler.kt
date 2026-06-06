package xyz.inorganic.quickmenu.ui.components

import kotlin.math.ceil

class MenuScrollHandler(
    private val rowHeight: Int,
    private val visibleRows: () -> Int,
    private val buttonsPerRow: () -> Int,
    private val getTotalItems: () -> Int
) {
    var scrollOffset = 0
        private set
    var isDraggingScrollbar = false
        private set

    private val totalRows: Int
        get() = ceil(getTotalItems().toDouble() / buttonsPerRow().toDouble()).toInt()

    val maxScroll: Int
        get() = maxOf(0, (totalRows * rowHeight) - (visibleRows() * rowHeight))

    fun isMouseOverScrollbar(mouseX: Double, mouseY: Double, scrollbarX: Int, scrollbarY: Int, scrollbarWidth: Int = 4): Boolean {
        if (totalRows <= visibleRows()) return false
        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= scrollbarY && mouseY <= scrollbarY + (visibleRows() * rowHeight)
    }

    fun updateScrollFromMouse(mouseY: Double, scrollbarY: Int) {
        val visibleH = visibleRows() * rowHeight
        val percentage = ((mouseY - scrollbarY) / visibleH.toDouble()).coerceIn(0.0, 1.0)
        scrollOffset = ((percentage * maxScroll).toInt() / rowHeight) * rowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
    }

    fun scroll(verticalAmount: Double) {
        scrollOffset = (scrollOffset - (verticalAmount * rowHeight).toInt()).coerceIn(0, maxScroll)
    }

    fun startDragging(mouseY: Double, scrollbarY: Int) {
        isDraggingScrollbar = true
        updateScrollFromMouse(mouseY, scrollbarY)
    }

    fun stopDragging() {
        isDraggingScrollbar = false
    }

    fun getScrollbarThumbY(scrollbarY: Int, scrollbarHeight: Int): Int {
        val thumbH = getScrollbarThumbHeight()
        val maxS = maxScroll
        return if (maxS > 0) scrollbarY + (scrollOffset.toDouble() / maxS.toDouble() * (scrollbarHeight - thumbH)).toInt() else scrollbarY
    }

    fun getScrollbarThumbHeight(): Int {
        val totalRowsVal = totalRows
        if (totalRowsVal <= visibleRows()) return 4
        return maxOf(4, (visibleRows().toDouble() / totalRowsVal.toDouble() * (visibleRows() * rowHeight)).toInt())
    }

    fun reset() {
        scrollOffset = 0
        isDraggingScrollbar = false
    }
}