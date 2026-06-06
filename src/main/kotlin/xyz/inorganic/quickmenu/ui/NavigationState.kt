package xyz.inorganic.quickmenu.ui

import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler

object NavigationState {
    private val navigationStack = mutableListOf<ActionButtonData>()

    fun currentFolder(): ActionButtonData? = navigationStack.lastOrNull()

    fun navigateTo(folder: ActionButtonData) = navigationStack.add(folder)

    fun navigateToLevel(index: Int) {
        if (index == -1) navigationStack.clear()
        else while (navigationStack.size > index + 1) navigationStack.removeAt(navigationStack.size - 1)
    }

    fun navigateRoot() = navigationStack.clear()

    fun getCurrentChildren(): MutableList<ActionButtonData> {
        return currentFolder()?.children ?: ActionButtonDataHandler.actions
    }

    fun isAtRoot(): Boolean = navigationStack.isEmpty()

    fun depth(): Int = navigationStack.size

    fun getStackItems(): List<Pair<Int, ActionButtonData>> {
        return navigationStack.mapIndexed { index, data -> index to data }
    }

    fun navigateBack() {
        if (navigationStack.size >= 2) {
            navigateToLevel(navigationStack.size - 2)
        } else {
            navigateRoot()
        }
    }
}