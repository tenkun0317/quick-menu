package xyz.inorganic.quickmenu.other

import xyz.inorganic.quickmenu.data.ActionButtonData

object KeybindConflicts {
    data class Entry(val data: ActionButtonData, val path: String)

    fun collectEntries(): List<Entry> {
        val result = mutableListOf<Entry>()
        fun walk(actions: List<ActionButtonData>, prefix: String) {
            for (action in actions) {
                val path = if (prefix.isEmpty()) action.name else "$prefix / ${action.name}"
                if (action.keybind.size >= 4) {
                    result.add(Entry(action, path))
                }
                if (action.isFolder) {
                    walk(action.children, path)
                }
            }
        }
        walk(ActionButtonDataHandler.actions, "")
        return result
    }

    fun conflictingKeybinds(exclude: ActionButtonData? = null): Set<List<Int>> {
        return collectEntries()
            .filter { it.data !== exclude }
            .groupingBy { it.data.keybind.toList() }
            .eachCount()
            .filterValues { it > 1 }
            .keys
    }

    fun isConflicting(data: ActionButtonData, exclude: ActionButtonData? = null): Boolean {
        if (data.keybind.size < 4) return false
        return conflictingKeybinds(exclude).contains(data.keybind.toList())
    }
}
