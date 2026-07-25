plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        string(current.version == "26.1" || current.version == "1.21.11") {
            replace("?.gui?.setScreen(", "?.setScreen(")
            replace(".gui.setScreen(", ".setScreen(")
            replace(".gui.screen()", ".screen")
        }
        string(current.version == "1.21.11") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("extractContents", "renderContents")
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
        }
    }
}
