package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.macros.MacroError
import xyz.inorganic.quickmenu.macros.MacroExecutor
import xyz.inorganic.quickmenu.macros.MacroParser

class TextMacroEditorUI(
    initialScript: String,
    private val onSave: (String) -> Unit
) : Screen(Component.translatable("menu.macro_editor.title")) {

    var previousScreen: Screen? = null

    private var scriptText = initialScript
    private var lastErrors: List<MacroError> = MacroParser.parse(initialScript).second

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 420
    private var menuHeight = 245

    private lateinit var runButton: Button

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - (menuHeight + 30)) / 2

        val box = MultiLineEditBox.builder()
            .setX(menuX + 10)
            .setY(menuY + 20)
            .build(font, menuWidth - 20, 150, Component.empty())
        box.setValue(scriptText)
        box.setValueListener { text ->
            scriptText = text
            lastErrors = MacroParser.parse(text).second
        }
        addRenderableWidget(box)

        runButton = Button.builder(Component.literal("▶ Run")) {
            if (MacroExecutor.testRunning()) {
                MacroExecutor.stopTest()
            } else {
                lastErrors = MacroParser.parse(scriptText).second
                MacroExecutor.testRun(scriptText)
            }
        }.pos(menuX + 10, menuY + 180).size(80, 20).build()
        addRenderableWidget(runButton)

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.finish")) {
            onSave(scriptText)
            onClose()
        }.pos(menuX + 230, menuY + 180).size(80, 20).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.cancel")) {
            onClose()
        }.pos(menuX + 330, menuY + 180).size(80, 20).build())
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val titleW = font.width(title)
        graphics.centeredText(font, title, (width - titleW) / 2, menuY - 15, -1)

        graphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0x33FFFFFF.toInt())
        graphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())
        graphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0x33FFFFFF.toInt())
        graphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())

        runButton.message = Component.literal(if (MacroExecutor.testRunning()) "⏸ Stop" else "▶ Run")

        lastErrors.take(3).forEachIndexed { i, err ->
            graphics.text(font, Component.literal(err.displayString()), menuX + 10, menuY + 208 + i * 10, 0xFFFF5555.toInt(), true)
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}