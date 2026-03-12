package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer

class BreadcrumbPopupUI(
    private val omittedFolders: List<Pair<Int, String>>, // Index in stack to Name
    private val onSelect: Consumer<Int>,
    private val previousScreen: Screen
) : Screen(Component.empty()) {
    
    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 150
    private var menuHeight = 0
    private val rowHeight = 22

    override fun init() {
        menuHeight = (omittedFolders.size * rowHeight + 20).coerceAtMost(height - 40)
        menuWidth = omittedFolders.maxOfOrNull { font.width(it.second) }?.plus(40)?.coerceAtLeast(120) ?: 150
        
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        omittedFolders.forEachIndexed { i, (level, name) ->
            val btnY = menuY + 10 + i * rowHeight
            if (btnY + 20 < menuY + menuHeight) {
                addRenderableWidget(Button.builder(Component.literal(name)) {
                    onSelect.accept(level)
                    onClose()
                }.pos(menuX + 10, btnY).size(menuWidth - 20, 20).build())
            }
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Draw background shadow
        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE121212.toInt())
        
        // Simple border
        val color = 0x44FFFFFF.toInt()
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, color)
        guiGraphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, color)
        guiGraphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, color)
        guiGraphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, color)
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        minecraft?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
