package xyz.inorganic.quickmenu.ui.components

import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler

class SearchHandler(
    private val onSearchChanged: () -> Unit
) {
    private var searchQuery = ""
    private var _searchBox: EditBox? = null
    val searchBox: EditBox? get() = _searchBox

    fun isActive(): Boolean = searchQuery.isNotEmpty()

    fun createSearchBox(font: net.minecraft.client.gui.Font, x: Int, y: Int, width: Int, height: Int, existingValue: String = ""): EditBox {
        searchQuery = existingValue
        _searchBox = EditBox(font, x, y, width, height, Component.empty()).apply {
            isBordered = false
            value = existingValue
            setResponder { newValue ->
                searchQuery = newValue
                onSearchChanged()
            }
        }
        return _searchBox!!
    }

    fun getFilteredActions(): List<ActionButtonData> {
        if (searchQuery.isEmpty()) return emptyList()
        
        val result = mutableListOf<ActionButtonData>()
        fun collect(actions: List<ActionButtonData>) {
            for (action in actions) {
                if (action.name.contains(searchQuery, ignoreCase = true)) {
                    result.add(action)
                }
                if (action.isFolder) {
                    collect(action.children)
                }
            }
        }
        collect(ActionButtonDataHandler.actions)
        return result
    }

    fun isSearchBoxFocused(): Boolean = _searchBox?.isFocused == true

    fun clear() {
        searchQuery = ""
        _searchBox?.value = ""
    }
    
    fun getExistingValue(): String = searchQuery
}