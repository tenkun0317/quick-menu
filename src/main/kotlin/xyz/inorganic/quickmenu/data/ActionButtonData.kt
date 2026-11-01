package xyz.inorganic.quickmenu.data

import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.command_actions.ActionData
import xyz.inorganic.quickmenu.other.ActionExecutor
import xyz.inorganic.quickmenu.other.ModConfig
import xyz.inorganic.quickmenu.other.RecentActions

class ActionButtonData(
    var name: String = "",
    var actions: MutableList<ActionData> = mutableListOf(),
    icon: ItemStack = ItemStack.EMPTY,
    keybind: MutableList<Int> = mutableListOf(),
    var isFolder: Boolean = false,
    var children: MutableList<ActionButtonData> = mutableListOf(),
    var registeredForRadial: Boolean = false
) {
    var keyPressed = false

    var iconString: String? = null
    var customModelDataString: String? = null

    private var _icon: ItemStack = icon

    var icon: ItemStack
        get() {
            if (_icon.isEmpty && iconString != null) {
                try {
                    val parts = iconString!!.split(":", limit = 2)
                    val identifier = Identifier.fromNamespaceAndPath(parts[0], parts[1])
                    val item = BuiltInRegistries.ITEM.getValue(identifier)
                    _icon = item.defaultInstance.copy()
                    if (customModelDataString != null && customModelDataString!!.isNotEmpty()) {
                        val cmdValues = CustomModelDataValues(customModelDataString!!)
                        _icon.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, cmdValues.getComponent())
                    }
                } catch (e: Exception) {
                    QuickMenu.LOGGER.warn("Failed to load icon for action \"$name\"", e)
                }
            }
            return _icon
        }
        set(value) {
            _icon = value
            iconString = null
        }

    var keybind: MutableList<Int> = keybind

    internal fun setIconFromLoad(icon: ItemStack, iconStr: String?, cmdStr: String?) {
        this._icon = icon
        this.iconString = iconStr
        this.customModelDataString = cmdStr
    }

    fun getKey(): InputConstants.Key? {
        if (keybind.size < 4) return null
        return InputConstants.Type.KEYSYM.getOrCreate(ActionKeyCodes.toNativeKey(keybind[0]))
    }

    fun run(isKeybind: Boolean = false) {
        if (isFolder && !isKeybind) return

        RecentActions.record(name)

        val displayRunText = QuickMenu.CONFIG.displayRunText
        if (displayRunText == ModConfig.DisplayRunText.ALWAYS || 
            (displayRunText == ModConfig.DisplayRunText.KEYBIND_ONLY && isKeybind)) {
            val client = Minecraft.getInstance()
            client.player?.sendSystemMessage(Component.literal("Ran action \"$name\""))
        }

        ActionExecutor.submit(this)
    }

    class CustomModelDataValues(cmdStr: String) {
        val stringList = listOf(cmdStr)
        var floatList = listOf<Float>()

        init {
            try {
                floatList = listOf(cmdStr.toFloat())
            } catch (ignored: Exception) {}
        }

        fun getComponent(): CustomModelData {
            return CustomModelData(floatList, emptyList(), stringList, emptyList())
        }
    }
}

object ActionKeyCodes {
    const val MOD_SHIFT = 1
    const val MOD_CONTROL = 2
    const val MOD_ALT = 4

    private const val SDL_INPUT = false

    private val legacyToSdl = buildMap {
        for (key in 65..90) put(key, key - 61)
        for (key in 49..57) put(key, key - 19)
        put(48, 39)
        for (key in 290..301) put(key, key - 232)
        for (key in 302..313) put(key, key - 198)
        for (key in 321..329) put(key, key - 232)
        putAll(mapOf(
            32 to 44, 39 to 52, 44 to 54, 45 to 45, 46 to 55, 47 to 56,
            59 to 51, 61 to 46, 91 to 47, 92 to 49, 93 to 48, 96 to 53,
            256 to 41, 257 to 40, 258 to 43, 259 to 42, 260 to 73, 261 to 76,
            262 to 79, 263 to 80, 264 to 81, 265 to 82, 266 to 75, 267 to 78,
            268 to 74, 269 to 77, 280 to 57, 281 to 71, 282 to 83,
            283 to 70, 284 to 72, 320 to 98, 330 to 99, 331 to 84,
            332 to 85, 333 to 86, 334 to 87, 335 to 88, 336 to 103,
            340 to 225, 341 to 224, 342 to 226, 343 to 227,
            344 to 229, 345 to 228, 346 to 230, 347 to 231, 348 to 118
        ))
    }
    private val sdlToLegacy = legacyToSdl.entries.associate { (legacy, sdl) -> sdl to legacy }

    fun toNativeKey(key: Int): Int {
        if (!SDL_INPUT) return if (key < -1) -1 else key
        return if (key < -1) -key - 2 else legacyToSdl[key] ?: 0
    }

    fun toStoredKey(key: Int): Int {
        if (!SDL_INPUT) return key
        return sdlToLegacy[key] ?: if (key > 0) -key - 2 else -1
    }

    fun toStoredModifiers(modifiers: Int): Int {
        if (!SDL_INPUT) return modifiers
        var stored = 0
        if (modifiers and 3 != 0) stored = stored or 1
        if (modifiers and 192 != 0) stored = stored or 2
        if (modifiers and 768 != 0) stored = stored or 4
        if (modifiers and 3072 != 0) stored = stored or 8
        return stored
    }

    fun toStoredMouseButton(button: Int): Int {
        if (!SDL_INPUT) return button
        return when (button) {
            1 -> 0
            2 -> 2
            3 -> 1
            else -> button - 1
        }
    }

    fun toNativeMouseButton(button: Int): Int {
        if (!SDL_INPUT) return button
        return when (button) {
            0 -> 1
            1 -> 3
            2 -> 2
            else -> button + 1
        }
    }

    fun isMouseButtonDown(client: Minecraft, button: Int): Boolean {
        if (button < 0) return false
        return GLFW.glfwGetMouseButton(client.window.handle(), button) == GLFW.GLFW_PRESS
    }
}
