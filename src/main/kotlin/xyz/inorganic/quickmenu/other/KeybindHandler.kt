package xyz.inorganic.quickmenu.other

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import xyz.inorganic.quickmenu.mixins.KeyBindingMixin

object KeybindHandler {
    private val queuedKeys = mutableListOf<KeyMapping>()
    private val queuedRelease = mutableListOf<KeyMapping>()
    private val heldKeys = mutableSetOf<KeyMapping>()
    private var didPress = false

    fun runQueue() {
        if (didPress) {
            for (keyMapping in queuedRelease) {
                if (keyMapping !in heldKeys) {
                    keyMapping.isDown = false
                }
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
        applyHeldKeys()
    }

    @JvmStatic
    fun applyHeldKeys() {
        for (keyMapping in heldKeys) {
            keyMapping.isDown = true
        }
    }

    fun pressKey(translationKey: String) {
        val keyMapping = getFromTranslationKey(translationKey) ?: return
        queuedKeys.add(keyMapping)
    }

    fun holdKey(translationKey: String) {
        val keyMapping = getFromTranslationKey(translationKey) ?: return
        if (heldKeys.add(keyMapping)) {
            val keyBindingMixin = keyMapping as KeyBindingMixin
            keyBindingMixin.setClickCount(1)
            keyMapping.isDown = true
        }
        queuedRelease.remove(keyMapping)
    }

    fun releaseKey(translationKey: String) {
        val keyMapping = getFromTranslationKey(translationKey) ?: return
        if (heldKeys.remove(keyMapping)) {
            val keyBindingMixin = keyMapping as KeyBindingMixin
            keyBindingMixin.setClickCount(0)
            keyMapping.isDown = false
        }
        queuedRelease.remove(keyMapping)
        queuedKeys.remove(keyMapping)
    }

    fun releaseAllHeld() {
        for (keyMapping in heldKeys) {
            keyMapping.isDown = false
        }
        heldKeys.clear()
        queuedRelease.clear()
        didPress = false
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
