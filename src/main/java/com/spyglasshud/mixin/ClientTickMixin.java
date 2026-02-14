package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfigScreen;
import com.spyglasshud.SpyglassKeyBinding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.screen == null && SpyglassKeyBinding.OPEN_CONFIG.consumeClick()) {
            client.setScreen(new SpyglassConfigScreen(null));
        }
    }
}
