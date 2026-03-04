package xyz.imcodist.quickmenu.other

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import xyz.imcodist.quickmenu.mixins.KeyBindingMixin

object KeybindHandler {
    private val queuedKeys = mutableListOf<KeyMapping>()
    private val queuedRelease = mutableListOf<KeyMapping>()
    private var didPress = false

    fun runQueue() {
        if (didPress) {
            for (keyMapping in queuedRelease) {
                keyMapping.isDown = false
            }
            didPress = false
            queuedRelease.clear()
        }

        for (keyMapping in queuedKeys) {
            val keyBindingMixin = keyMapping as KeyBindingMixin
            keyBindingMixin.setClickCount(1)
            keyMapping.isDown = true
            didPress = true
            queuedRelease.add(keyMapping)
        }

        queuedKeys.clear()
    }

    fun pressKey(translationKey: String) {
        val keyMapping = getFromTranslationKey(translationKey) ?: return
        queuedKeys.add(keyMapping)
    }

    fun getFromTranslationKey(translationKey: String): KeyMapping? {
        val client = Minecraft.getInstance() ?: return null
        return client.options.keyMappings.find { it.name == translationKey }
    }

    fun getKeybindings(): Array<KeyMapping>? {
        val client = Minecraft.getInstance() ?: return null
        return client.options.keyMappings
    }
}
