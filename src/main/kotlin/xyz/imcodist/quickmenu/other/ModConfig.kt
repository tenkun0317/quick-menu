package xyz.imcodist.quickmenu.other

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class ModConfig {
    enum class DisplayRunText {
        ALWAYS, KEYBIND_ONLY, NEVER
    }

    // Menu settings
    var menuWidth: Int = 180
    var menuHeight: Int = 114
    var buttonsPerRow: Int = 5
    var closeOnKeyReleased: Boolean = false
    var hideEditIcon: Boolean = false

    // Action button settings
    var closeOnAction: Boolean = true
    var showActionsInTooltip: Boolean = true
    var displayRunText: DisplayRunText = DisplayRunText.KEYBIND_ONLY

    // Accessor methods to mimic owo-lib generated methods (to avoid breaking UI code)
    fun menuWidth(): Int = menuWidth
    fun menuHeight(): Int = menuHeight
    fun buttonsPerRow(): Int = buttonsPerRow
    fun closeOnKeyReleased(): Boolean = closeOnKeyReleased
    fun hideEditIcon(): Boolean = hideEditIcon
    fun closeOnAction(): Boolean = closeOnAction
    fun showActionsInTooltip(): Boolean = showActionsInTooltip
    fun displayRunText(): DisplayRunText = displayRunText

    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private val CONFIG_FILE: File = FabricLoader.getInstance().configDir.resolve("quickmenu.json").toFile()

        fun createAndLoad(): ModConfig {
            if (!CONFIG_FILE.exists()) {
                val config = ModConfig()
                config.save()
                return config
            }
            return try {
                val config = GSON.fromJson(CONFIG_FILE.readText(), ModConfig::class.java)
                config ?: ModConfig()
            } catch (e: Exception) {
                e.printStackTrace()
                ModConfig()
            }
        }
    }

    fun save() {
        try {
            CONFIG_FILE.writeText(GSON.toJson(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
