package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfigScreen;
import com.spyglasshud.SpyglassKeyBinding;
import com.spyglasshud.SpyglassZoom;
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
        boolean isScoping = client.player != null && client.player.isScoping();
        SpyglassZoom.onTick(isScoping);
        if (SpyglassKeyBinding.OPEN_CONFIG.consumeClick()) {
            if (client.gui.screen() instanceof SpyglassConfigScreen) {
                client.gui.setScreen(null);
            } else if (client.gui.screen() == null) {
                client.gui.setScreen(new SpyglassConfigScreen(null));
            }
        }
    }
}
