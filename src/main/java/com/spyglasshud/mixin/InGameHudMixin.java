package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

    /**
     * Checks if the player is currently using a spyglass.
     */
    private boolean shouldHideHud() {
        Minecraft client = Minecraft.getInstance();
        return SpyglassConfig.get().isHideHud() && client.player != null && client.player.isScoping();
    }

    // --- Reduce spyglass overlay scale so the scope border fits on screen ---
    @ModifyArg(method = "renderCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSpyglassOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V"), index = 1)
    private float adjustSpyglassScale(float scopeScale) {
        return scopeScale * (float) SpyglassConfig.get().getOverlayScale();
    }

    // --- Hide crosshair when using spyglass ---
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideCrosshair(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide hotbar when using spyglass ---
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void hideHotbar(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide status bars (hearts, hunger, armor, air) ---
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide mount health (when riding a horse etc.) ---
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void hideMountHealth(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide status effect overlay ---
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void hideStatusEffects(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide held item tooltip ---
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void hideHeldItemTooltip(CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }
}
