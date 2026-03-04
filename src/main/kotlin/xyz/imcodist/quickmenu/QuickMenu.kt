package xyz.imcodist.quickmenu

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import xyz.imcodist.quickmenu.logic.KeybindManager
import xyz.imcodist.quickmenu.other.ActionButtonDataHandler
import xyz.imcodist.quickmenu.other.ModConfig
import xyz.imcodist.quickmenu.other.ModKeybindings

class QuickMenu : ModInitializer {
    companion object {
        val CONFIG: ModConfig = ModConfig.createAndLoad()
    }

    override fun onInitialize() {
        ModKeybindings.initialize()
        ActionButtonDataHandler.initialize()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            KeybindManager.onClientTick(client)
        }
    }
}
