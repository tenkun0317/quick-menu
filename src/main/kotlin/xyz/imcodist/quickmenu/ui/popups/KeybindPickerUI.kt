package xyz.imcodist.quickmenu.ui.popups

import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.imcodist.quickmenu.other.KeybindHandler
import java.util.function.Consumer

class KeybindPickerUI : Screen(Component.empty()) {
    var onSelectedKeybind: Consumer<KeyMapping> = Consumer {}
    var previousScreen: Screen? = null

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 320
    private var menuHeight = 240
    
    private var scrollOffset = 0
    private val entryHeight = 22

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2
        rebuildList()
    }

    private fun rebuildList() {
        clearWidgets()
        val keyBindings = KeybindHandler.getKeybindings()
        if (keyBindings != null) {
            val startX = menuX + 10
            val startY = menuY + 10
            
            keyBindings.forEachIndexed { index, keyBinding ->
                val btnY = startY + index * entryHeight - scrollOffset
                
                if (btnY >= startY && btnY + 20 <= startY + menuHeight - 20) {
                    val btn = Button.builder(Component.translatable(keyBinding.name)) {
                        onSelectedKeybind.accept(keyBinding)
                        onClose()
                    }.pos(startX, btnY).size(menuWidth - 20, 20).build()
                    addRenderableWidget(btn)
                }
            }
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val keyBindings = KeybindHandler.getKeybindings()
        if (keyBindings != null) {
            val maxScroll = maxOf(0, keyBindings.size * entryHeight - (menuHeight - 20))
            scrollOffset = (scrollOffset - (verticalAmount * entryHeight).toInt()).coerceIn(0, maxScroll)
            rebuildList()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC000000.toInt())
        super.render(context, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
