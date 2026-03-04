package xyz.imcodist.quickmenu.other

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import xyz.imcodist.quickmenu.data.*
import java.io.File

object ActionButtonDataHandler {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    val actions = mutableListOf<ActionButtonData>()
    
    private val configFile: File
        get() = FabricLoader.getInstance().configDir.resolve("quickmenu_data.json").toFile()

    fun initialize() {
        load()
    }

    fun add(action: ActionButtonData) {
        actions.add(action)
        save()
    }

    fun remove(action: ActionButtonData) {
        actions.remove(action)
        save()
    }

    fun load() {
        val file = configFile
        if (file.exists()) {
            try {
                val content = file.readText()
                val jsonList = json.decodeFromString<List<ActionButtonDataJSON>>(content)
                actions.clear()
                actions.addAll(jsonList.map { it.toActionButtonData() })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun save() {
        try {
            val jsonList = actions.map { it.toJSON() }
            val content = json.encodeToString(jsonList)
            configFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
