package xyz.inorganic.quickmenu.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.data.command_actions.CommandActionData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton
import xyz.inorganic.quickmenu.ui.popups.ActionPickerUI
import xyz.inorganic.quickmenu.ui.popups.ItemPickerUI
import xyz.inorganic.quickmenu.ui.popups.KeybindPickerUI

class ActionEditorUI(private val originalAction: ActionButtonData? = null) : Screen(Component.translatable("menu.editor.title")) {
    private var actionButtonData = ActionButtonData()
    private var isNewAction = true
    var previousScreen: Screen? = null

    private lateinit var nameEditBox: EditBox
    private lateinit var customModelDataEditBox: EditBox
    private lateinit var keybindBtn: Button
    
    private var settingKeybind = false
    private var isBoundKeybind = false
    private var keybind = mutableListOf<Int>()

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 260
    private var menuHeight = 180 
    
    private var scrollOffset = 0
    private val actionRowHeight = 25
    private var isDraggingScrollbar = false

    init {
        originalAction?.let {
            actionButtonData = ActionButtonData(it.name, it.actions.toMutableList(), it.icon.copy(), it.keybind.toMutableList())
            keybind.addAll(it.keybind)
            if (keybind.size >= 4) isBoundKeybind = true
            isNewAction = false
        }
    }

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - (menuHeight + 40)) / 2 + 10

        // Name
        nameEditBox = EditBox(font, menuX + 100, menuY + 15, 140, 20, Component.empty())
        nameEditBox.value = actionButtonData.name
        addRenderableWidget(nameEditBox)

        // Icon
        val iconButton = QuickMenuButton(actionButtonData.icon, {
            syncInputToData()
            val itemPicker = ItemPickerUI()
            itemPicker.previousScreen = this
            itemPicker.onSelectedItem = { item ->
                actionButtonData.icon = item
                rebuildWidgets()
            }
            minecraft?.setScreen(itemPicker)
        })
        iconButton.x = menuX + 100
        iconButton.y = menuY + 40
        addRenderableWidget(iconButton)

        // CustomModelData
        customModelDataEditBox = EditBox(font, menuX + 100, menuY + 70, 140, 20, Component.empty())
        customModelDataEditBox.value = getCustomModelData(actionButtonData.icon)
        customModelDataEditBox.setResponder { updateCustomModelData() }
        addRenderableWidget(customModelDataEditBox)

        // Keybind
        keybindBtn = Button.builder(Component.empty()) {
            syncInputToData()
            settingKeybind = true
            updateKeybindLabel()
        }.pos(menuX + 100, menuY + 95).size(140, 20).build()
        addRenderableWidget(keybindBtn)
        updateKeybindLabel()

        rebuildActionList()

        // Footer buttons
        val footerY = menuY + menuHeight + 10
        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.finish")) {
            saveAndClose()
        }.pos(menuX + 40, footerY).size(80, 20).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.cancel")) {
            onClose()
        }.pos(menuX + 140, footerY).size(80, 20).build())
    }

    private fun rebuildActionList() {
        val listStartY = menuY + 125
        val maxVisibleHeight = 50 
        
        children().filterIsInstance<AbstractWidget>().filter { 
            it.y >= listStartY && it.y < menuY + menuHeight 
        }.forEach { removeWidget(it) }
        
        actionButtonData.actions.forEachIndexed { index, action ->
            val rowY = listStartY + index * actionRowHeight - scrollOffset
            
            if (rowY >= listStartY && rowY + 20 <= listStartY + maxVisibleHeight) {
                if (action is CommandActionData) {
                    val cmdBox = EditBox(font, menuX + 10, rowY, 190, 20, Component.empty())
                    cmdBox.value = action.command
                    cmdBox.setResponder { actionButtonData.actions[index] = CommandActionData(it) }
                    addRenderableWidget(cmdBox)
                } else if (action is KeybindActionData) {
                    val kbBtn = Button.builder(Component.translatable("text.action.key", getReadableKeyName(action.translationKey))) {
                        syncInputToData()
                        val picker = KeybindPickerUI()
                        picker.previousScreen = this
                        picker.onSelectedKeybind = { mapping ->
                            actionButtonData.actions[index] = KeybindActionData(mapping.name)
                            rebuildWidgets()
                        }
                        minecraft?.setScreen(picker)
                    }.pos(menuX + 10, rowY).size(190, 20).build()
                    addRenderableWidget(kbBtn)
                }

                addRenderableWidget(Button.builder(Component.literal("-")) {
                    actionButtonData.actions.removeAt(index)
                    rebuildWidgets()
                }.pos(menuX + 205, rowY).size(20, 20).build())
            }
        }

        val addActionY = listStartY + actionButtonData.actions.size * actionRowHeight - scrollOffset
        if (addActionY >= listStartY && addActionY + 20 <= listStartY + maxVisibleHeight) {
            addRenderableWidget(Button.builder(Component.translatable("menu.main.button.add_action")) {
                syncInputToData()
                val picker = ActionPickerUI()
                picker.previousScreen = this
                picker.onSelectedAction = { selected ->
                    actionButtonData.actions.add(selected)
                    rebuildWidgets()
                }
                minecraft?.setScreen(picker)
            }.pos(menuX + 10, addActionY).size(80, 20).build())
        }
    }

    private fun isMouseOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        if (totalHeight <= 50) return false
        val sbX = menuX + menuWidth - 8
        val sbY = menuY + 121
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= sbY && mouseY <= sbY + 54
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        val maxScroll = maxOf(0, totalHeight - 50)
        val sbY = menuY + 121
        val percentage = ((mouseY - sbY) / 54.0).coerceIn(0.0, 1.0)
        scrollOffset = (percentage * maxScroll).toInt()
        scrollOffset = (scrollOffset / actionRowHeight) * actionRowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        rebuildWidgets()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        // Handle Keybind capturing
        if (settingKeybind) {
            if (event.button() <= 2) {
                isBoundKeybind = true
                keybind.apply { clear(); add(event.button()); add(0); add(0); add(1) }
            }
            settingKeybind = false
            updateKeybindLabel()
            return true
        }

        // Handle Scrollbar dragging
        if (isMouseOverScrollbar(event.x(), event.y())) {
            isDraggingScrollbar = true
            updateScrollFromMouse(event.y())
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(event.y())
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        isDraggingScrollbar = false
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        syncInputToData()
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        val maxScroll = maxOf(0, totalHeight - 50)
        scrollOffset = (scrollOffset - (verticalAmount * actionRowHeight).toInt()).coerceIn(0, maxScroll)
        rebuildWidgets()
        return true
    }

    private fun syncInputToData() {
        if (::nameEditBox.isInitialized) actionButtonData.name = nameEditBox.value
        updateCustomModelData()
    }

    private fun updateKeybindLabel() {
        var msg = if (!isBoundKeybind) "Not Bound" else {
            if (keybind[3] == 0) {
                val base = InputConstants.Type.KEYSYM.getOrCreate(keybind[0]).displayName.string
                val mods = keybind[2]
                val sb = StringBuilder()
                if (mods and GLFW.GLFW_MOD_CONTROL != 0) sb.append("Ctrl+")
                if (mods and GLFW.GLFW_MOD_SHIFT != 0) sb.append("Shift+")
                if (mods and GLFW.GLFW_MOD_ALT != 0) sb.append("Alt+")
                sb.append(base)
                sb.toString()
            } else "Mouse ${keybind[0]}"
        }
        if (settingKeybind) msg = "> $msg <"
        keybindBtn.message = Component.literal(msg)
    }

    private fun getReadableKeyName(translationKey: String): String {
        return Component.translatable(translationKey).string
    }

    private fun getCustomModelData(item: ItemStack): String {
        if (item.isEmpty) return ""
        val cmd = item.get(DataComponents.CUSTOM_MODEL_DATA) ?: return ""
        return if (cmd.strings().isNotEmpty()) cmd.strings()[0] else ""
    }

    private fun updateCustomModelData() {
        if (!::customModelDataEditBox.isInitialized) return
        val text = customModelDataEditBox.value
        if (actionButtonData.icon.isEmpty) return
        try {
            if (text.isNotEmpty()) {
                val values = ActionButtonData.CustomModelDataValues(text)
                actionButtonData.icon.set(DataComponents.CUSTOM_MODEL_DATA, values.getComponent())
            } else {
                actionButtonData.icon.remove(DataComponents.CUSTOM_MODEL_DATA)
            }
        } catch (ignored: Exception) {}
    }

    override fun rebuildWidgets() {
        clearWidgets()
        init()
    }

    private fun saveAndClose() {
        syncInputToData()
        actionButtonData.keybind = if (isBoundKeybind) keybind.toMutableList() else mutableListOf()

        if (isNewAction) ActionButtonDataHandler.add(actionButtonData)
        else {
            originalAction?.let {
                it.name = actionButtonData.name
                it.actions = actionButtonData.actions
                it.icon = actionButtonData.icon
                it.keybind = actionButtonData.keybind
            }
            ActionButtonDataHandler.save()
        }
        onClose()
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (settingKeybind) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                if (event.key() != GLFW.GLFW_KEY_LEFT_CONTROL && event.key() != GLFW.GLFW_KEY_RIGHT_CONTROL &&
                    event.key() != GLFW.GLFW_KEY_LEFT_SHIFT && event.key() != GLFW.GLFW_KEY_RIGHT_SHIFT &&
                    event.key() != GLFW.GLFW_KEY_LEFT_ALT && event.key() != GLFW.GLFW_KEY_RIGHT_ALT) {
                    
                    isBoundKeybind = true
                    keybind.apply { 
                        clear()
                        add(event.key())
                        add(event.scancode())
                        add(event.modifiers())
                        add(0) 
                    }
                    settingKeybind = false
                    updateKeybindLabel()
                }
            } else {
                isBoundKeybind = false
                settingKeybind = false
                updateKeybindLabel()
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val titleWidth = font.width(title)
        guiGraphics.drawString(font, title, (width - titleWidth) / 2, menuY - 15, -1, true)

        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())

        super.render(guiGraphics, mouseX, mouseY, partialTick)
        
        val sepTopY = menuY + 120
        guiGraphics.fill(menuX + 1, sepTopY, menuX + menuWidth - 1, sepTopY + 1, 0x44FFFFFF.toInt())

        // --- Scrollbar Drawing ---
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        if (totalHeight > 50) {
            val sbX = menuX + menuWidth - 6
            val sbY = menuY + 121
            val sbH = 54
            guiGraphics.fill(sbX, sbY, sbX + 3, sbY + sbH, 0x22FFFFFF.toInt())
            val thumbH = maxOf(4, (50.0 / totalHeight.toDouble() * sbH).toInt())
            val thumbY = sbY + (scrollOffset.toDouble() / (totalHeight - 50).toDouble() * (sbH - thumbH)).toInt()
            val thumbColor = if (isDraggingScrollbar || isMouseOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            guiGraphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, thumbColor)
        }

        // --- Scroll Fades ---
        val listStartY = menuY + 121
        val maxVisibleHeight = 54
        val maxScroll = maxOf(0, totalHeight - 50)

        if (scrollOffset > 0) guiGraphics.fillGradient(menuX + 1, listStartY, menuX + menuWidth - 1, listStartY + 12, 0x99000000.toInt(), 0x00000000.toInt())
        if (scrollOffset < maxScroll) {
            val fadeBottomY = menuY + menuHeight - 1
            guiGraphics.fillGradient(menuX + 1, fadeBottomY - 12, menuX + menuWidth - 1, fadeBottomY, 0x00000000.toInt(), 0x99000000.toInt())
        }

        guiGraphics.drawString(font, Component.translatable("menu.editor.property.name"), menuX + 10, menuY + 20, 0xFFAAAAAA.toInt(), true)
        guiGraphics.drawString(font, Component.translatable("menu.editor.property.icon"), menuX + 10, menuY + 45, 0xFFAAAAAA.toInt(), true)
        guiGraphics.drawString(font, "CustomModelData", menuX + 10, menuY + 75, 0xFFAAAAAA.toInt(), true)
        guiGraphics.drawString(font, Component.translatable("menu.editor.property.keybind"), menuX + 10, menuY + 100, 0xFFAAAAAA.toInt(), true)
    }

    override fun onClose() {
        minecraft?.setScreen(previousScreen) ?: super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}
