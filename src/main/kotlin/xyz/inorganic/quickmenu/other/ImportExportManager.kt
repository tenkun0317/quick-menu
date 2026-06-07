package xyz.inorganic.quickmenu.other

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.ActionButtonDataJSON
import xyz.inorganic.quickmenu.data.toJSON

object ImportExportManager {
    private const val MAGIC_HEADER = "QMENU_V1"

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    sealed class Result {
        data class Success(val count: Int) : Result()
        data class Failure(val reason: String) : Result()
    }

    fun exportToString(): String {
        val jsonList = ActionButtonDataHandler.actions.map { it.toJSON() }
        val payload = json.encodeToString(jsonList)
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))
        return "$MAGIC_HEADER:$encoded"
    }

    fun copyToClipboard(): Result {
        return try {
            val client = Minecraft.getInstance()
            client.keyboardHandler.setClipboard(exportToString())
            Result.Success(ActionButtonDataHandler.actions.size)
        } catch (e: Exception) {
            Result.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    fun applyImportFromClipboard(): Result {
        val raw = try {
            Minecraft.getInstance().keyboardHandler.getClipboard()
        } catch (e: Exception) {
            null
        }
        if (raw.isNullOrBlank()) return Result.Failure("Clipboard is empty")
        return applyImport(raw, xyz.inorganic.quickmenu.other.ModConfig.ImportMode.REPLACE_ALL)
    }

    fun peekCount(text: String): Result {
        return try {
            val list = decodePayload(text)
            Result.Success(list.size)
        } catch (e: ImportException) {
            Result.Failure(e.message ?: "Import failed")
        } catch (e: Exception) {
            Result.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    fun applyImport(text: String, mode: xyz.inorganic.quickmenu.other.ModConfig.ImportMode): Result {
        val parsed = try {
            decodePayload(text)
        } catch (e: ImportException) {
            return Result.Failure(e.message ?: "Import failed")
        } catch (e: Exception) {
            return Result.Failure(e.message ?: e.javaClass.simpleName)
        }

        when (mode) {
            xyz.inorganic.quickmenu.other.ModConfig.ImportMode.REPLACE_ALL -> {
                ActionButtonDataHandler.actions.clear()
                ActionButtonDataHandler.actions.addAll(parsed)
            }
            xyz.inorganic.quickmenu.other.ModConfig.ImportMode.ADD_ONLY -> {
                ActionButtonDataHandler.actions.addAll(parsed)
            }
            xyz.inorganic.quickmenu.other.ModConfig.ImportMode.MERGE_BY_NAME -> {
                mergeByName(ActionButtonDataHandler.actions, parsed)
            }
            xyz.inorganic.quickmenu.other.ModConfig.ImportMode.ASK -> {
                return Result.Failure("ASK mode requires explicit selection")
            }
        }

        ActionButtonDataHandler.save()
        return Result.Success(parsed.size)
    }

    private fun mergeByName(existing: MutableList<ActionButtonData>, incoming: List<ActionButtonData>) {
        for (incomingItem in incoming) {
            val matchIndex = existing.indexOfFirst { it.name == incomingItem.name }
            if (matchIndex >= 0) {
                existing[matchIndex] = incomingItem
            } else {
                existing.add(incomingItem)
            }
        }
    }

    private class ImportException(message: String) : Exception(message)

    private fun decodePayload(text: String): List<ActionButtonData> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw ImportException("Empty data")

        val payload = if (trimmed.startsWith("$MAGIC_HEADER:")) {
            val encoded = trimmed.substring(MAGIC_HEADER.length + 1)
            try {
                String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                throw ImportException("Invalid base64 encoding")
            }
        } else {
            trimmed
        }

        val list = try {
            json.decodeFromString<List<ActionButtonDataJSON>>(payload)
        } catch (e: Exception) {
            throw ImportException("Invalid JSON: ${e.message ?: e.javaClass.simpleName}")
        }

        return list.map { it.toActionButtonData() }
    }
}
