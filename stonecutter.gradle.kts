plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        if (current.version == "26.3") {
            string(true) {
                replace("import org.lwjgl.glfw.GLFW", "import com.mojang.blaze3d.platform.InputConstants as GLFW")
                replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
                replace("GLFW.GLFW_KEY_LEFT_CONTROL", "GLFW.KEY_LCONTROL")
                replace("GLFW.GLFW_KEY_RIGHT_CONTROL", "GLFW.KEY_RCONTROL")
                replace("GLFW.GLFW_KEY_LEFT_SHIFT", "GLFW.KEY_LSHIFT")
                replace("GLFW.GLFW_KEY_RIGHT_SHIFT", "GLFW.KEY_RSHIFT")
                replace("GLFW.GLFW_KEY_LEFT_ALT", "GLFW.KEY_LALT")
                replace("GLFW.GLFW_KEY_RIGHT_ALT", "GLFW.KEY_RALT")
                replace("GLFW.GLFW_KEY_", "GLFW.KEY_")
                replace("GLFW.GLFW_MOD_", "GLFW.MOD_")
                replace("GLFW.GLFW_MOUSE_BUTTON_2", "GLFW.MOUSE_BUTTON_RIGHT")
                replace("InputConstants.isKeyDown(client.window, ", "InputConstants.isKeyDown(")
                replace("InputConstants.isKeyDown(window, ", "InputConstants.isKeyDown(")
                replace("InputConstants.isKeyDown(w, ", "InputConstants.isKeyDown(")
                replace("event.scancode()", "event.keycode()")
                replace("return GLFW.glfwGetMouseButton(client.window.handle(), button) == GLFW.GLFW_PRESS", "return org.lwjgl.sdl.SDLMouse.SDL_GetMouseState(null, null) and (1 shl (toNativeMouseButton(button) - 1)) != 0")
                replace("private const val SDL_INPUT = false", "private const val SDL_INPUT = true")
            }
        }
        string(current.version == "26.1" || current.version == "1.21.11") {
            replace("?.gui?.setScreen(", "?.setScreen(")
            replace(".gui.setScreen(", ".setScreen(")
            replace("client.gui.screen()", "client.screen")
        }
        string(current.version == "1.21.11") {
            replace("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper")
            replace("KeyMappingHelper.registerKeyMapping(", "KeyBindingHelper.registerKeyBinding(")
            replace("override fun extractRenderState(graphics: GuiGraphicsExtractor", "override fun render(graphics: GuiGraphics")
            replace("super.extractRenderState(graphics: GuiGraphicsExtractor", "super.render(graphics: GuiGraphics")
            replace("super.extractRenderState(", "super.render(")
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("extractContents", "renderContents")
            replace("graphics.centeredText(", "graphics.drawCenteredString(")
            replace("""context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, sourceX.toFloat(), sourceY.toFloat(), sourceWidth, sourceHeight, textureWidth, textureHeight)""", """context.blit(TEXTURE, x, y, sourceX, sourceY, sourceWidth.toFloat(), sourceHeight.toFloat(), textureWidth.toFloat(), textureHeight.toFloat())""")
            replace("graphics.text(", "graphics.drawString(")
            replace("graphics.item(", "graphics.renderItem(")
            replace("graphics.itemDecorations(", "graphics.renderItemDecorations(")
            replace("context.centeredText(", "context.drawCenteredString(")
            replace("context.text(", "context.drawString(")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.export.success", result.count))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.export.success", result.count), false)""")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.export.failure", result.reason))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.export.failure", result.reason), false)""")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.import.failure", "Clipboard is empty"))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.import.failure", "Clipboard is empty"), false)""")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.import.failure", peek.reason))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.import.failure", peek.reason), false)""")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.import.success", result.count))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.import.success", result.count), false)""")
            replace("""minecraft?.player?.sendSystemMessage(Component.translatable("menu.main.import.failure", result.reason))""", """minecraft?.player?.displayClientMessage(Component.translatable("menu.main.import.failure", result.reason), false)""")
            replace("""client.player?.sendSystemMessage(Component.literal("Ran action \"${'$'}name\""))""", """client.player?.displayClientMessage(Component.literal("Ran action \"${'$'}name\""), false)""")
            replace("""client.player?.sendSystemMessage(Component.literal("[QuickMenu] ${'$'}text"))""", """client.player?.displayClientMessage(Component.literal("[QuickMenu] ${'$'}text"), false)""")
        }
    }
}
