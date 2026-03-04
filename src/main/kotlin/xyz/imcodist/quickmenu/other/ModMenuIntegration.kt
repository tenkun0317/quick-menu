package xyz.imcodist.quickmenu.other

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.network.chat.Component
import xyz.imcodist.quickmenu.QuickMenu

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            val config = QuickMenu.CONFIG
            val builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.config.quickmenu.title"))
                .setSavingRunnable { config.save() }

            val entryBuilder = builder.entryBuilder()

            val menuCategory = builder.getOrCreateCategory(Component.translatable("text.config.quickmenu.section.menu"))

            menuCategory.addEntry(entryBuilder.startIntField(Component.translatable("text.config.quickmenu.option.menuWidth"), config.menuWidth)
                .setDefaultValue(180)
                .setMin(60)
                .setMax(450)
                .setSaveConsumer { config.menuWidth = it }
                .build())

            menuCategory.addEntry(entryBuilder.startIntField(Component.translatable("text.config.quickmenu.option.menuHeight"), config.menuHeight)
                .setDefaultValue(114)
                .setMin(60)
                .setMax(450)
                .setSaveConsumer { config.menuHeight = it }
                .build())

            menuCategory.addEntry(entryBuilder.startIntSlider(Component.translatable("text.config.quickmenu.option.buttonsPerRow"), config.buttonsPerRow, 1, 14)
                .setDefaultValue(5)
                .setSaveConsumer { config.buttonsPerRow = it }
                .build())

            menuCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.config.quickmenu.option.closeOnKeyReleased"), config.closeOnKeyReleased)
                .setDefaultValue(false)
                .setSaveConsumer { config.closeOnKeyReleased = it }
                .build())

            menuCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.config.quickmenu.option.hideEditIcon"), config.hideEditIcon)
                .setDefaultValue(false)
                .setSaveConsumer { config.hideEditIcon = it }
                .build())

            val actionCategory = builder.getOrCreateCategory(Component.translatable("text.config.quickmenu.section.action_buttons"))

            actionCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.config.quickmenu.option.closeOnAction"), config.closeOnAction)
                .setDefaultValue(true)
                .setSaveConsumer { config.closeOnAction = it }
                .build())

            actionCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("text.config.quickmenu.option.showActionsInTooltip"), config.showActionsInTooltip)
                .setDefaultValue(true)
                .setSaveConsumer { config.showActionsInTooltip = it }
                .build())

            actionCategory.addEntry(entryBuilder.startEnumSelector(Component.translatable("text.config.quickmenu.option.displayRunText"), ModConfig.DisplayRunText::class.java, config.displayRunText)
                .setDefaultValue(ModConfig.DisplayRunText.KEYBIND_ONLY)
                .setSaveConsumer { config.displayRunText = it }
                .build())

            builder.build()
        }
    }
}
