package xyz.inorganic.quickmenu.other

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class ModConfig {
    enum class DisplayRunText {
        ALWAYS, KEYBIND_ONLY, NEVER
    }

    enum class ImportMode {
        ASK, REPLACE_ALL, MERGE_BY_NAME, ADD_ONLY
    }

    enum class RadialDisplayMode {
        STATIC, DYNAMIC
    }

    // Menu size based on button counts
    var buttonsPerRow: Int = 5
    var visibleRows: Int = 2
    
    var closeOnKeyReleased: Boolean = false
    var hideEditIcon: Boolean = false

    // Action button settings
    var closeOnAction: Boolean = true
    var showActionsInTooltip: Boolean = true
    var displayRunText: DisplayRunText = DisplayRunText.KEYBIND_ONLY
    
    var keepNavigationHistory: Boolean = false

    // Import/Export
    var defaultImportMode: ImportMode = ImportMode.ASK

    // Edit mode modifiers (Stored as key name strings)
    var moveModifier: String = "key.keyboard.left.control"
    var deleteModifier: String = "key.keyboard.left.shift"

    // Radial menu settings
    var radialMaxItems: Int = 8
    var radialDisplayMode: RadialDisplayMode = RadialDisplayMode.STATIC
    var radialRadius: Int = 60
    var radialDeadZoneRadius: Int = 15
    var radialCloseOnAction: Boolean = true

    // Helper methods for backward compatibility if needed
    fun buttonsPerRow(): Int = buttonsPerRow
    fun visibleRows(): Int = visibleRows
    fun closeOnKeyReleased(): Boolean = closeOnKeyReleased
    fun hideEditIcon(): Boolean = hideEditIcon
    fun closeOnAction(): Boolean = closeOnAction
    fun showActionsInTooltip(): Boolean = showActionsInTooltip
    fun displayRunText(): DisplayRunText = displayRunText
    fun keepNavigationHistory(): Boolean = keepNavigationHistory
    fun defaultImportMode(): ImportMode = defaultImportMode
    fun moveModifier(): String = moveModifier
    fun deleteModifier(): String = deleteModifier

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