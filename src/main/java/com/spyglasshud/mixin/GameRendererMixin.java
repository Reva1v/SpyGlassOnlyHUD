package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class GameRendererMixin {

    // calculateFov() returns FOV in degrees with spyglass zoom already applied (×0.1 modifier).
    // Rescale relative to ×10 baseline: multiplier = 10.0 / currentZoom, so DEFAULT_ZOOM (10) = no change.
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void modifySpyglassFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.screen == null) {
            float fov = cir.getReturnValue();
            cir.setReturnValue(fov * (float) (10.0 / SpyglassZoom.get(partialTick)));
        }
    }
}
