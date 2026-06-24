package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer

class BreadcrumbPopupUI(
    private val omittedFolders: List<Pair<Int, String>>,
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

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE121212.toInt())

        val color = 0x44FFFFFF.toInt()
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, color)
        graphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, color)
        graphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, color)
        graphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, color)

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}