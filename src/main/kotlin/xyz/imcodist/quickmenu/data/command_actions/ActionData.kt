package xyz.imcodist.quickmenu.data.command_actions

import kotlinx.serialization.Serializable

@Serializable
sealed class ActionData {
    abstract val type: String
    abstract val value: String
    
    abstract val typeString: String
    abstract fun getDisplayString(): String
    
    abstract fun run()
}
