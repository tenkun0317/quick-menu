package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.other.ModConfig
import java.util.function.Consumer

class ImportConfirmScreen(
    private val buttonCount: Int,
    private val onSelect: Consumer<ModConfig.ImportMode>,
    private val previousScreen: Screen
) : Screen(Component.translatable("menu.main.import.confirm.title")) {

    private var menuX = 0
    private var menuY = 0
    private val menuWidth = 240
    private val menuHeight = 180

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        val btnWidth = menuWidth - 20
        val btnX = menuX + 10
        val btnH = 20
        val gap = 6
        val startY = menuY + 50

        addRenderableWidget(Button.builder(Component.translatable("menu.main.import.mode.replace_all")) {
            onSelect.accept(ModConfig.ImportMode.REPLACE_ALL)
            onClose()
        }.pos(btnX, startY).size(btnWidth, btnH).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.main.import.mode.merge_by_name")) {
            onSelect.accept(ModConfig.ImportMode.MERGE_BY_NAME)
            onClose()
        }.pos(btnX, startY + (btnH + gap)).size(btnWidth, btnH).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.main.import.mode.add_only")) {
            onSelect.accept(ModConfig.ImportMode.ADD_ONLY)
            onClose()
        }.pos(btnX, startY + 2 * (btnH + gap)).size(btnWidth, btnH).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.main.import.cancel")) {
            onClose()
        }.pos(btnX, startY + 3 * (btnH + gap)).size(btnWidth, btnH).build())
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE121212.toInt())

        val borderColor = 0x44FFFFFF.toInt()
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, borderColor)
        graphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, borderColor)
        graphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, borderColor)
        graphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, borderColor)

        val titleW = font.width(title)
        graphics.centeredText(font, title, width / 2, menuY + 12, -1)
        val message = Component.translatable("menu.main.import.confirm.choose", buttonCount)
        graphics.centeredText(font, message, width / 2, menuY + 30, 0xFFAAAAAA.toInt())

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
