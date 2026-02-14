package com.spyglasshud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /**
     * Checks if the player is currently using a spyglass.
     */
    private boolean isUsingSpyglass() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.player.isUsingSpyglass();
    }

    // --- Hide crosshair when using spyglass ---
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideCrosshair(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide hotbar when using spyglass ---
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hideHotbar(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide status bars (hearts, hunger, armor, air) ---
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide experience bar ---
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hideExperienceBar(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide experience level ---
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void hideExperienceLevel(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide mount health (when riding a horse etc.) ---
    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void hideMountHealth(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide status effect overlay ---
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hideStatusEffects(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide held item tooltip ---
    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void hideHeldItemTooltip(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }
}
