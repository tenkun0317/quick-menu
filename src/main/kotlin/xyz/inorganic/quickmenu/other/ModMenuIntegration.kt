package xyz.inorganic.quickmenu.other

import com.mojang.blaze3d.platform.InputConstants
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.KeyCodeControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            createConfigScreen(parent)
        }
    }

    private fun createConfigScreen(parent: Screen?): Screen {
        val config = QuickMenu.CONFIG
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("text.config.quickmenu.title"))
            .save { config.save() }
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.menu"))
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.buttonsPerRow"))
                    .controller { IntegerFieldControllerBuilder.create(it, 1, 15) }
                    .binding(5, { config.buttonsPerRow }, { config.buttonsPerRow = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.visibleRows"))
                    .controller { IntegerFieldControllerBuilder.create(it, 1, 10) }
                    .binding(2, { config.visibleRows }, { config.visibleRows = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnKeyReleased"))
                    .controller { opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true) }
                    .binding(false, { config.closeOnKeyReleased }, { config.closeOnKeyReleased = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.hideEditIcon"))
                    .controller { opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true) }
                    .binding(false, { config.hideEditIcon }, { config.hideEditIcon = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.keepNavigationHistory"))
                    .controller { opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true) }
                    .binding(false, { config.keepNavigationHistory }, { config.keepNavigationHistory = it })
                    .build())
                .option(Option.createBuilder<InputConstants.Key>()
                    .name(Component.translatable("text.config.quickmenu.option.moveModifier"))
                    .controller { KeyCodeControllerBuilder.create(it) }
                    .binding(
                        InputConstants.getKey("key.keyboard.left.control"),
                        { InputConstants.getKey(config.moveModifier) },
                        { config.moveModifier = it.name }
                    )
                    .build())
                .option(Option.createBuilder<InputConstants.Key>()
                    .name(Component.translatable("text.config.quickmenu.option.deleteModifier"))
                    .controller { KeyCodeControllerBuilder.create(it) }
                    .binding(
                        InputConstants.getKey("key.keyboard.left.shift"),
                        { InputConstants.getKey(config.deleteModifier) },
                        { config.deleteModifier = it.name }
                    )
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.action_buttons"))
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnAction"))
                    .controller { opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true) }
                    .binding(true, { config.closeOnAction }, { config.closeOnAction = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.showActionsInTooltip"))
                    .controller { opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true) }
                    .binding(true, { config.showActionsInTooltip }, { config.showActionsInTooltip = it })
                    .build())
                .option(Option.createBuilder<ModConfig.DisplayRunText>()
                    .name(Component.translatable("text.config.quickmenu.option.displayRunText"))
                    .binding(ModConfig.DisplayRunText.KEYBIND_ONLY, { config.displayRunText }, { config.displayRunText = it })
                    .build())
                .build())
            .build()
            .generateScreen(parent)
    }
}