package xyz.inorganic.quickmenu.ui.components

class MenuScrollHandler(
    private val rowHeight: Int,
    private val getTotalHeight: () -> Int,
    private val getVisibleHeight: () -> Int
) {
    var scrollOffset = 0
        private set
    var isDraggingScrollbar = false
        private set

    private val visibleHeight: Int
        get() = getVisibleHeight()

    val maxScroll: Int
        get() = maxOf(0, getTotalHeight() - visibleHeight)

    fun hasScrollbar(): Boolean = maxScroll > 0

    fun isMouseOverScrollbar(
        mouseX: Double,
        mouseY: Double,
        scrollbarX: Int,
        scrollbarY: Int,
        scrollbarHeight: Int,
        scrollbarWidth: Int = 4
    ): Boolean {
        if (!hasScrollbar()) return false
        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
            mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight
    }

    fun updateScrollFromMouse(mouseY: Double, scrollbarY: Int, scrollbarHeight: Int) {
        val percentage = ((mouseY - scrollbarY) / scrollbarHeight.toDouble()).coerceIn(0.0, 1.0)
        setScroll((percentage * maxScroll).toInt())
    }

    fun scroll(verticalAmount: Double) {
        setScroll(scrollOffset - (verticalAmount * rowHeight).toInt())
    }

    fun setScroll(offset: Int) {
        scrollOffset = offset.coerceIn(0, maxScroll)
    }

    fun startDragging() {
        isDraggingScrollbar = true
    }

    fun stopDragging() {
        isDraggingScrollbar = false
    }

    fun thumbHeight(scrollbarHeight: Int): Int {
        val total = getTotalHeight()
        if (total <= 0) return scrollbarHeight
        return maxOf(4, (visibleHeight.toDouble() / total.toDouble() * scrollbarHeight).toInt())
    }

    fun thumbY(scrollbarY: Int, scrollbarHeight: Int): Int {
        val thumbH = thumbHeight(scrollbarHeight)
        val maxS = maxScroll
        if (maxS <= 0) return scrollbarY
        return scrollbarY + (scrollOffset.toDouble() / maxS.toDouble() * (scrollbarHeight - thumbH)).toInt()
    }

    fun reset() {
        scrollOffset = 0
        isDraggingScrollbar = false
    }
}
