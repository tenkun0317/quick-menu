package xyz.inorganic.quickmenu.other

import xyz.inorganic.quickmenu.QuickMenu

object RecentActions {
    private const val MAX_ENTRIES = 5

    fun record(name: String) {
        if (name.isEmpty()) return
        val list = QuickMenu.CONFIG.recentActions
        list.remove(name)
        list.add(0, name)
        while (list.size > MAX_ENTRIES) {
            list.removeAt(list.size - 1)
        }
        QuickMenu.CONFIG.save()
    }

    fun resolve(): List<String> = QuickMenu.CONFIG.recentActions.toList()

    fun clear() {
        QuickMenu.CONFIG.recentActions.clear()
        QuickMenu.CONFIG.save()
    }
}
