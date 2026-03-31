package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // getFov() already applies vanilla ×10 spyglass zoom. Rescale relative to that
    // baseline: multiplier = 10.0 / currentZoom, so DEFAULT_ZOOM (10) = no change.
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifySpyglassFov(Camera camera, float partialTick, boolean useFovSetting,
                                    CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.screen == null) {
            float fov = cir.getReturnValue();
            cir.setReturnValue(fov * (float) (10.0 / SpyglassZoom.get()));
        }
    }
}
