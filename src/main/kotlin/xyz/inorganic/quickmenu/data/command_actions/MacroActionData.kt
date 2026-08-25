package xyz.inorganic.quickmenu.data.command_actions

import kotlinx.serialization.Serializable
import xyz.inorganic.quickmenu.macros.MacroParser

@Serializable
class MacroActionData(
    val script: String = ""
) : ActionData() {
    override val type: String = "macro"
    override val value: String = script

    override val typeString: String = "MACRO"
    override fun getDisplayString(): String = MacroParser.previewLine(script)

    override fun run() {}
}