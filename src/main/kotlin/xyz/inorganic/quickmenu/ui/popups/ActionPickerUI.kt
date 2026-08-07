package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.data.command_actions.CommandActionData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.data.command_actions.SleepActionData
import java.util.function.Consumer

class ActionPickerUI : Screen(Component.translatable("menu.action_picker.title")) {
    var onSelectedAction: Consumer<ActionData> = Consumer {}
    var previousScreen: Screen? = null

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 180
    private var menuHeight = 150

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        val startY = menuY + 10

        val commandBtn = Button.builder(Component.translatable("menu.action_picker.command")) {
            onSelectedAction.accept(CommandActionData())
            onClose()
        }.pos(menuX + 10, startY).size(160, 20).build()
        addRenderableWidget(commandBtn)

        val keybindBtn = Button.builder(Component.translatable("menu.action_picker.keybind")) {
            onSelectedAction.accept(KeybindActionData())
            onClose()
        }.pos(menuX + 10, startY + 25).size(160, 20).build()
        addRenderableWidget(keybindBtn)

        val sleepBtn = Button.builder(Component.translatable("menu.action_picker.sleep")) {
            onSelectedAction.accept(SleepActionData(10))
            onClose()
        }.pos(menuX + 10, startY + 50).size(160, 20).build()
        addRenderableWidget(sleepBtn)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}