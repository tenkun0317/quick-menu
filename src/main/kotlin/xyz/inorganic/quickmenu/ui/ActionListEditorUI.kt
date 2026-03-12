package xyz.inorganic.quickmenu.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.CommandActionData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.ui.popups.ActionPickerUI
import xyz.inorganic.quickmenu.ui.popups.KeybindPickerUI
import kotlin.math.ceil

class ActionListEditorUI(private val actionButtonData: ActionButtonData) : Screen(Component.literal("Edit Actions")) {
    var previousScreen: ActionEditorUI? = null

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 260
    private var menuHeight = 180
    
    private var scrollOffset = 0
    private val actionRowHeight = 25
    private var isDraggingScrollbar = false

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - (menuHeight + 40)) / 2

        rebuildActionList()

        // Done Button
        addRenderableWidget(Button.builder(Component.literal("Done")) {
            onClose()
        }.pos(menuX + (menuWidth - 80) / 2, menuY + menuHeight + 10).size(80, 20).build())
    }

    private fun rebuildActionList() {
        clearWidgets()
        
        val listStartY = menuY + 10
        val maxVisibleHeight = menuHeight - 20
        
        actionButtonData.actions.forEachIndexed { index, action ->
            val rowY = listStartY + index * actionRowHeight - scrollOffset
            
            if (rowY >= listStartY && rowY + 20 <= listStartY + maxVisibleHeight) {
                if (action is CommandActionData) {
                    val cmdBox = EditBox(font, menuX + 10, rowY, 190, 20, Component.empty())
                    
                    // Dynamic limit helper
                    fun updateLimit(text: String) {
                        cmdBox.setMaxLength(if (text.startsWith("/")) 32767 else 256)
                    }
                    
                    updateLimit(action.command)
                    cmdBox.value = action.command
                    cmdBox.setResponder { 
                        updateLimit(it)
                        actionButtonData.actions[index] = CommandActionData(it) 
                    }
                    addRenderableWidget(cmdBox)
                } else if (action is KeybindActionData) {
                    val kbBtn = Button.builder(Component.literal("Key: ${Component.translatable(action.translationKey).string}")) {
                        val picker = KeybindPickerUI()
                        picker.previousScreen = this
                        picker.onSelectedKeybind = { mapping ->
                            actionButtonData.actions[index] = KeybindActionData(mapping.name)
                            init()
                        }
                        minecraft?.setScreen(picker)
                    }.pos(menuX + 10, rowY).size(190, 20).build()
                    addRenderableWidget(kbBtn)
                }

                addRenderableWidget(Button.builder(Component.literal("-")) {
                    actionButtonData.actions.removeAt(index)
                    init()
                }.pos(menuX + 205, rowY).size(20, 20).build())
            }
        }

        val addActionY = listStartY + actionButtonData.actions.size * actionRowHeight - scrollOffset
        if (addActionY >= listStartY && addActionY + 20 <= listStartY + maxVisibleHeight) {
            addRenderableWidget(Button.builder(Component.literal("+ Add Action")) {
                val picker = ActionPickerUI()
                picker.previousScreen = this
                picker.onSelectedAction = { selected ->
                    actionButtonData.actions.add(selected)
                    init()
                }
                minecraft?.setScreen(picker)
            }.pos(menuX + 10, addActionY).size(80, 20).build())
        }
    }

    private fun isMouseOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        if (totalHeight <= menuHeight - 20) return false
        val sbX = menuX + menuWidth - 8
        val sbY = menuY + 10
        return mouseX >= sbX && mouseX <= sbX + 4 && mouseY >= sbY && mouseY <= sbY + (menuHeight - 20)
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        val visibleHeight = menuHeight - 20
        val maxScroll = maxOf(0, totalHeight - visibleHeight)
        val sbY = menuY + 10
        val percentage = ((mouseY - sbY) / visibleHeight.toDouble()).coerceIn(0.0, 1.0)
        scrollOffset = (percentage * maxScroll).toInt()
        scrollOffset = (scrollOffset / actionRowHeight) * actionRowHeight
        scrollOffset = scrollOffset.coerceIn(0, maxScroll)
        init()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
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
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        val maxScroll = maxOf(0, totalHeight - (menuHeight - 20))
        scrollOffset = (scrollOffset - (verticalAmount * actionRowHeight).toInt()).coerceIn(0, maxScroll)
        init()
        return true
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val titleW = font.width(title)
        guiGraphics.drawString(font, title, (width - titleW) / 2, menuY - 15, -1, true)
        
        guiGraphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0x33FFFFFF.toInt())
        guiGraphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())

        super.render(guiGraphics, mouseX, mouseY, partialTick)

        // Scrollbar
        val totalHeight = (actionButtonData.actions.size + 1) * actionRowHeight
        val visibleH = menuHeight - 20
        if (totalHeight > visibleH) {
            val sbX = menuX + menuWidth - 6
            val sbY = menuY + 10
            guiGraphics.fill(sbX, sbY, sbX + 3, sbY + visibleH, 0x22FFFFFF.toInt())
            val thumbH = maxOf(4, (visibleH.toDouble() / totalHeight.toDouble() * visibleH).toInt())
            val thumbY = sbY + (scrollOffset.toDouble() / (totalHeight - visibleH).toDouble() * (visibleH - thumbH)).toInt()
            val thumbColor = if (isDraggingScrollbar || isMouseOverScrollbar(mouseX.toDouble(), mouseY.toDouble())) 0xAAFFFFFF.toInt() else 0x66FFFFFF.toInt()
            guiGraphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, thumbColor)
        }

        // Fades
        if (scrollOffset > 0) {
            guiGraphics.fillGradient(menuX + 1, menuY + 1, menuX + menuWidth - 1, menuY + 13, 0x99000000.toInt(), 0x00000000.toInt())
        }
        val maxScroll = maxOf(0, totalHeight - visibleH)
        if (scrollOffset < maxScroll) {
            val fadeY = menuY + menuHeight - 1
            guiGraphics.fillGradient(menuX + 1, fadeY - 12, menuX + menuWidth - 1, fadeY, 0x00000000.toInt(), 0x99000000.toInt())
        }
    }

    override fun onClose() {
        minecraft?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
