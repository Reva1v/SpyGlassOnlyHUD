package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfig;
import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xDelta, double yDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.screen == null
                && SpyglassConfig.get().isZoomEnabled()) {
            SpyglassZoom.adjust(yDelta);
            ci.cancel();
        }
    }
}
