package xyz.inorganic.quickmenu.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Checkbox
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
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton
import xyz.inorganic.quickmenu.ui.popups.ItemPickerUI

class ActionEditorUI(private val originalAction: ActionButtonData? = null) : Screen(Component.translatable("menu.editor.title")) {
    private var actionButtonData = ActionButtonData()
    private var isNewAction = true
    var previousScreen: Screen? = null

    private lateinit var nameEditBox: EditBox
    private lateinit var customModelDataEditBox: EditBox
    private lateinit var keybindBtn: Button
    private lateinit var folderCheckbox: Checkbox
    
    private var settingKeybind = false
    private var isBoundKeybind = false
    private var keybind = mutableListOf<Int>()

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 260
    private var menuHeight = 150 

    init {
        originalAction?.let {
            actionButtonData = ActionButtonData(it.name, it.actions.toMutableList(), it.icon.copy(), it.keybind.toMutableList(), it.isFolder, it.children.toMutableList())
            keybind.addAll(it.keybind)
            if (keybind.size >= 4) isBoundKeybind = true
            isNewAction = false
        }
    }

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - (menuHeight + 40)) / 2

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

        // Folder Checkbox
        folderCheckbox = Checkbox.builder(Component.literal("Folder"), font)
            .pos(menuX + 10, menuY + 120)
            .selected(actionButtonData.isFolder)
            .onValueChange { _, value -> 
                toggleFolderMode(value)
            }
            .build()
        addRenderableWidget(folderCheckbox)

        // Edit Actions Button
        if (!actionButtonData.isFolder) {
            addRenderableWidget(Button.builder(Component.literal("Edit Actions >")) {
                syncInputToData()
                val listEditor = ActionListEditorUI(actionButtonData)
                listEditor.previousScreen = this
                minecraft?.setScreen(listEditor)
            }.pos(menuX + 100, menuY + 120).size(140, 20).build())
        }

        // Footer buttons
        val footerY = menuY + menuHeight + 5
        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.finish")) {
            saveAndClose()
        }.pos(menuX + 40, footerY).size(80, 20).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.cancel")) {
            onClose()
        }.pos(menuX + 140, footerY).size(80, 20).build())
    }

    private fun toggleFolderMode(isFolder: Boolean) {
        if (!isFolder && actionButtonData.children.isNotEmpty()) {
            val warningScreen = ConfirmScreen({ confirmed ->
                if (confirmed) {
                    // When converting folder -> action, we move children to the parent list later during save
                    actionButtonData.isFolder = false
                    rebuildWidgets()
                }
                minecraft?.setScreen(this)
            }, Component.literal("Convert to Action"), Component.literal("This folder has items. Move them to the parent list?"))
            minecraft?.setScreen(warningScreen)
        } else {
            actionButtonData.isFolder = isFolder
            rebuildWidgets()
        }
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

        val parentList = MainUI.currentFolder()?.children ?: ActionButtonDataHandler.actions

        if (isNewAction) {
            parentList.add(actionButtonData)
        } else {
            originalAction?.let {
                it.name = actionButtonData.name
                it.actions = actionButtonData.actions
                it.icon = actionButtonData.icon
                it.keybind = actionButtonData.keybind
                
                // If we converted folder -> action, move children to parent list
                if (it.isFolder && !actionButtonData.isFolder && actionButtonData.children.isNotEmpty()) {
                    val index = parentList.indexOf(it)
                    if (index != -1) {
                        parentList.addAll(index + 1, actionButtonData.children)
                    } else {
                        parentList.addAll(actionButtonData.children)
                    }
                    actionButtonData.children.clear()
                }
                
                it.isFolder = actionButtonData.isFolder
                it.children = actionButtonData.children
            }
        }
        
        ActionButtonDataHandler.save()
        onClose()
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (settingKeybind) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                if (event.key() != GLFW.GLFW_KEY_LEFT_CONTROL && event.key() != GLFW.GLFW_KEY_RIGHT_CONTROL &&
                    event.key() != GLFW.GLFW_KEY_LEFT_SHIFT && event.key() != GLFW.GLFW_KEY_RIGHT_SHIFT &&
                    event.key() != GLFW.GLFW_KEY_LEFT_ALT && event.key() != GLFW.GLFW_KEY_RIGHT_ALT) {
                    isBoundKeybind = true
                    keybind.apply { clear(); add(event.key()); add(event.scancode()); add(event.modifiers()); add(0) }
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

    // Inner helper for confirmation screen - Optimized for 1.21.11
    class ConfirmScreen(val callback: (Boolean) -> Unit, title: Component, val message: Component) : Screen(title) {
        override fun init() {
            addRenderableWidget(Button.builder(Component.literal("Yes")) { 
                callback(true) 
            }.pos(width / 2 - 105, height / 2 + 10).size(100, 20).build())
            
            addRenderableWidget(Button.builder(Component.literal("No")) { 
                callback(false) 
            }.pos(width / 2 + 5, height / 2 + 10).size(100, 20).build())
        }
        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            // Fill background manually to avoid click-blocking overlay issues
            guiGraphics.fill(0, 0, width, height, 0xCC000000.toInt())
            guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 30, -1)
            guiGraphics.drawCenteredString(font, message, width / 2, height / 2 - 15, -1)
            super.render(guiGraphics, mouseX, mouseY, partialTick)
        }
    }
}
