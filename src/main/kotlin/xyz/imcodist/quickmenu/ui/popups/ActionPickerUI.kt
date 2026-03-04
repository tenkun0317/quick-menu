package xyz.imcodist.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.imcodist.quickmenu.data.command_actions.ActionData
import xyz.imcodist.quickmenu.data.command_actions.CommandActionData
import xyz.imcodist.quickmenu.data.command_actions.KeybindActionData
import xyz.imcodist.quickmenu.ui.surfaces.SwitcherSurface
import java.util.function.Consumer

class ActionPickerUI : Screen(Component.empty()) {
    var onSelectedAction: Consumer<ActionData> = Consumer {}
    var previousScreen: Screen? = null

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 180
    private var menuHeight = 120

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - menuHeight) / 2

        val startY = menuY + 10
        
        val commandBtn = Button.builder(Component.literal("Command")) {
            onSelectedAction.accept(CommandActionData())
            onClose()
        }.pos(menuX + 10, startY).size(160, 20).build()
        addRenderableWidget(commandBtn)

        val keybindBtn = Button.builder(Component.literal("Keybind")) {
            onSelectedAction.accept(KeybindActionData())
            onClose()
        }.pos(menuX + 10, startY + 25).size(160, 20).build()
        addRenderableWidget(keybindBtn)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
