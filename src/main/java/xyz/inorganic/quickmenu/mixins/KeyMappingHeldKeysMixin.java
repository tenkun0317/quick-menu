package xyz.inorganic.quickmenu.mixins;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.inorganic.quickmenu.other.KeybindHandler;

@Mixin(KeyMapping.class)
public class KeyMappingHeldKeysMixin {
    @Inject(method = "setAll", at = @At("RETURN"))
    private static void quickmenu$reapplyHeldKeys(CallbackInfo ci) {
        KeybindHandler.applyHeldKeys();
    }
}