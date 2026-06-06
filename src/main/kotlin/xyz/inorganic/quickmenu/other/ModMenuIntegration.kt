package xyz.inorganic.quickmenu.other

import com.mojang.blaze3d.platform.InputConstants
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
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
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(15)
                    }
                    .binding(5, { config.buttonsPerRow }, { config.buttonsPerRow = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.visibleRows"))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(10)
                    }
                    .binding(2, { config.visibleRows }, { config.visibleRows = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnKeyReleased"))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.closeOnKeyReleased }, { config.closeOnKeyReleased = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.hideEditIcon"))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.hideEditIcon }, { config.hideEditIcon = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.keepNavigationHistory"))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.keepNavigationHistory }, { config.keepNavigationHistory = it })
                    .build())
                .option(Option.createBuilder<String>()
                    .name(Component.translatable("text.config.quickmenu.option.moveModifier"))
                    .binding(config.moveModifier, { config.moveModifier }, { config.moveModifier = it })
                    .build())
                .option(Option.createBuilder<String>()
                    .name(Component.translatable("text.config.quickmenu.option.deleteModifier"))
                    .binding(config.deleteModifier, { config.deleteModifier }, { config.deleteModifier = it })
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.action_buttons"))
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnAction"))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.closeOnAction }, { config.closeOnAction = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.showActionsInTooltip"))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.showActionsInTooltip }, { config.showActionsInTooltip = it })
                    .build())
                .option(Option.createBuilder<ModConfig.DisplayRunText>()
                    .name(Component.translatable("text.config.quickmenu.option.displayRunText"))
                    .controller { opt ->
                        EnumControllerBuilder.create(opt)
                            .enumClass(ModConfig.DisplayRunText::class.java)
                    }
                    .binding(ModConfig.DisplayRunText.KEYBIND_ONLY, { config.displayRunText }, { config.displayRunText = it })
                    .build())
                .build())
            .build()
            .generateScreen(parent)
    }
}