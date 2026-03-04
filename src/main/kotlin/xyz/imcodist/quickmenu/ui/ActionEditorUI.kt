package xyz.imcodist.quickmenu.ui

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
import xyz.imcodist.quickmenu.QuickMenu
import xyz.imcodist.quickmenu.data.ActionButtonData
import xyz.imcodist.quickmenu.data.command_actions.ActionData
import xyz.imcodist.quickmenu.data.command_actions.CommandActionData
import xyz.imcodist.quickmenu.data.command_actions.KeybindActionData
import xyz.imcodist.quickmenu.other.ActionButtonDataHandler
import xyz.imcodist.quickmenu.ui.components.QuickMenuButton
import xyz.imcodist.quickmenu.ui.popups.ActionPickerUI
import xyz.imcodist.quickmenu.ui.popups.ItemPickerUI
import xyz.imcodist.quickmenu.ui.popups.KeybindPickerUI

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
    private var menuHeight = 205 // さらにコンパクトに短縮
    
    private var scrollOffset = 0
    private val actionRowHeight = 25

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
        menuY = (height - menuHeight) / 2

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

        // Bottom buttons - 座標をさらに上に詰める
        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.finish")) {
            saveAndClose()
        }.pos(menuX + 40, menuY + menuHeight - 25).size(80, 20).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.cancel")) {
            onClose()
        }.pos(menuX + 140, menuY + menuHeight - 25).size(80, 20).build())
    }

    private fun rebuildActionList() {
        val listStartY = menuY + 125
        val maxVisibleHeight = 50 
        
        children().filterIsInstance<AbstractWidget>().filter { 
            it.y >= listStartY && it.y < menuY + menuHeight - 25 
        }.forEach { removeWidget(it) }
        
        actionButtonData.actions.forEachIndexed { index, action ->
            val rowY = listStartY + index * actionRowHeight - scrollOffset
            
            if (rowY >= listStartY && rowY + 20 <= listStartY + maxVisibleHeight) {
                if (action is CommandActionData) {
                    val cmdBox = EditBox(font, menuX + 10, rowY, 200, 20, Component.empty())
                    cmdBox.value = action.command
                    cmdBox.setResponder { actionButtonData.actions[index] = CommandActionData(it) }
                    addRenderableWidget(cmdBox)
                } else if (action is KeybindActionData) {
                    val kbBtn = Button.builder(Component.literal("Key: ${getReadableKeyName(action.translationKey)}")) {
                        syncInputToData()
                        val picker = KeybindPickerUI()
                        picker.previousScreen = this
                        picker.onSelectedKeybind = { mapping ->
                            actionButtonData.actions[index] = KeybindActionData(mapping.name)
                            rebuildWidgets()
                        }
                        minecraft?.setScreen(picker)
                    }.pos(menuX + 10, rowY).size(200, 20).build()
                    addRenderableWidget(kbBtn)
                }

                addRenderableWidget(Button.builder(Component.literal("-")) {
                    actionButtonData.actions.removeAt(index)
                    rebuildWidgets()
                }.pos(menuX + 215, rowY).size(20, 20).build())
            }
        }

        val addActionY = listStartY + actionButtonData.actions.size * actionRowHeight - scrollOffset
        if (addActionY >= listStartY && addActionY + 20 <= listStartY + maxVisibleHeight) {
            addRenderableWidget(Button.builder(Component.literal("+ Action")) {
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

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (settingKeybind) {
            if (event.button() <= 2) {
                isBoundKeybind = true
                keybind.apply { clear(); add(event.button()); add(0); add(0); add(1) }
            }
            settingKeybind = false
            updateKeybindLabel()
            return true
        }
        return super.mouseClicked(event, doubleClick)
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
