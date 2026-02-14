package com.spyglasshud.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

    /**
     * Checks if the player is currently using a spyglass.
     */
    private boolean isUsingSpyglass() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.isScoping();
    }

    // --- Hide crosshair when using spyglass ---
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideCrosshair(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide hotbar when using spyglass ---
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void hideHotbar(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide status bars (hearts, hunger, armor, air) ---
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide mount health (when riding a horse etc.) ---
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void hideMountHealth(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide status effect overlay ---
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void hideStatusEffects(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }

    // --- Hide held item tooltip ---
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void hideHeldItemTooltip(CallbackInfo ci) {
        if (isUsingSpyglass()) {
            ci.cancel();
        }
    }
}
