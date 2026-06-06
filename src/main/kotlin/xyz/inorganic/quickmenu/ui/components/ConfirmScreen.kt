package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ConfirmScreen(
    val callback: (Boolean) -> Unit,
    title: Component,
    private val message: Component
) : Screen(title) {

    override fun init() {
        addRenderableWidget(Button.builder(Component.literal("Yes")) {
            callback(true)
        }.pos(width / 2 - 105, height / 2 + 10).size(100, 20).build())

        addRenderableWidget(Button.builder(Component.literal("No")) {
            callback(false)
        }.pos(width / 2 + 5, height / 2 + 10).size(100, 20).build())
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xCC000000.toInt())
        graphics.centeredText(font, title, width / 2, height / 2 - 30, -1)
        graphics.centeredText(font, message, width / 2, height / 2 - 15, -1)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
}