package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton
import xyz.inorganic.quickmenu.ui.surfaces.SwitcherSurface
import java.util.function.Consumer
import kotlin.math.ceil

class ItemPickerUI : Screen(Component.empty()) {
    var selectedItem: ItemStack = ItemStack.EMPTY
    var onSelectedItem: Consumer<ItemStack> = Consumer {}
    var previousScreen: Screen? = null

    private lateinit var searchBox: EditBox
    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 230
    private var menuHeight = 210
    
    private var scrollOffset = 0
    private val buttonSize = 26

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        searchBox = EditBox(font, menuX + 10, menuY + 10, menuWidth - 20, 20, Component.empty())
        searchBox.setResponder { 
            scrollOffset = 0
            updateItems() 
        }
        addRenderableWidget(searchBox)

        updateItems()
    }

    private fun updateItems() {
        clearWidgets()
        addRenderableWidget(searchBox)

        val search = searchBox.value.lowercase()
        val items = BuiltInRegistries.ITEM.filter { item ->
            item.getName(item.defaultInstance).string.lowercase().contains(search)
        }

        val rowSize = 8
        val startX = menuX + 10
        val startY = menuY + 40
        val visibleHeight = menuHeight - 50

        items.forEachIndexed { index, item ->
            val row = index / rowSize
            val col = index % rowSize
            
            val btnX = startX + col * buttonSize
            val btnY = startY + row * buttonSize - scrollOffset
            
            if (btnY >= startY && btnY + buttonSize <= startY + visibleHeight) {
                val stack = item.defaultInstance
                val button = QuickMenuButton(stack, {
                        selectedItem = stack
                        onClose()
                })
                button.x = btnX
                button.y = btnY
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(stack.hoverName))
                addRenderableWidget(button)
            }
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val search = searchBox.value.lowercase()
        val itemsCount = BuiltInRegistries.ITEM.filter { it.getName(it.defaultInstance).string.lowercase().contains(search) }.size
        val totalRows = ceil(itemsCount.toDouble() / 8.0).toInt()
        val totalHeight = totalRows * buttonSize
        val visibleHeight = menuHeight - 50
        
        val maxScroll = maxOf(0, totalHeight - visibleHeight)
        scrollOffset = (scrollOffset - (verticalAmount * buttonSize).toInt()).coerceIn(0, maxScroll)
        updateItems()
        return true
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC000000.toInt())
        super.render(context, mouseX, mouseY, delta)
    }

    override fun onClose() {
        onSelectedItem.accept(selectedItem)
        minecraft?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
