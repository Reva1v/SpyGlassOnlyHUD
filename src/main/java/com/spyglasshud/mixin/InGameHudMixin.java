package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class InGameHudMixin {

    /**
     * Checks if the player is currently using a spyglass.
     */
    private boolean shouldHideHud() {
        Minecraft client = Minecraft.getInstance();
        return SpyglassConfig.get().isHideHud() && client.player != null && client.player.isScoping();
    }

    // --- Reduce spyglass overlay scale so the scope border fits on screen ---
    @ModifyArg(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSpyglassOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"), index = 1)
    private float adjustSpyglassScale(float scopeScale) {
        return scopeScale * (float) SpyglassConfig.get().getOverlayScale();
    }

    // --- Hide crosshair when using spyglass ---
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide hotbar when using spyglass ---
    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void hideHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide status bars (hearts, hunger, armor, air) ---
    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide mount health (when riding a horse etc.) ---
    @Inject(method = "extractVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void hideMountHealth(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide status effect overlay ---
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void hideStatusEffects(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }

    // --- Hide held item tooltip ---
    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void hideHeldItemTooltip(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        if (shouldHideHud()) {
            ci.cancel();
        }
    }
}
