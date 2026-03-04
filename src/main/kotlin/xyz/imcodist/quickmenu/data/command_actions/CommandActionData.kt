package xyz.imcodist.quickmenu.data.command_actions

import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft

@Serializable
class CommandActionData(
    val command: String = ""
) : ActionData() {
    override val type: String = "cmd"
    override val value: String = command
    
    override val typeString: String = "CMD"
    override fun getDisplayString(): String = command
    
    override fun run() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        
        var commandToRun = command
        if (commandToRun.startsWith("/")) {
            commandToRun = commandToRun.substring(1)
            player.connection.sendCommand(commandToRun)
        } else {
            if (commandToRun.length >= 256) {
                commandToRun = commandToRun.substring(0, 256)
            }
            player.connection.sendChat(commandToRun)
        }
    }
}
