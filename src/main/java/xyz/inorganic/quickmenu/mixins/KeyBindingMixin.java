package xyz.inorganic.quickmenu.mixins;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyBindingMixin {
    @Accessor("clickCount")
    void setClickCount(int clickCount);

    @Accessor("key")
    Key getKey();
}
