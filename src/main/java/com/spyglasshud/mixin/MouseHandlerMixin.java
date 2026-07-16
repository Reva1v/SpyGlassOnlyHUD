package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassConfig;
import com.spyglasshud.SpyglassZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xDelta, double yDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.screen == null
                && SpyglassConfig.get().isZoomEnabled()) {
            SpyglassZoom.adjust(yDelta);
            ci.cancel();
        }
    }

    // Scale the raw mouse deltas down before turnPlayer consumes them, so aiming stays
    // controllable at high zoom. Factor = baseline zoom / current zoom, clamped to <=1 so
    // zooming in slows the mouse but zooming out never speeds it past normal.
    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void slowMouseWhileZooming(double partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isScoping() && client.gui.screen() == null
                && SpyglassConfig.get().isZoomEnabled() && SpyglassConfig.get().isSlowMouseWhileZooming()) {
            double factor = Math.min(1.0, SpyglassZoom.DEFAULT_ZOOM / SpyglassZoom.getCurrent());
            setAccumulatedDX(getAccumulatedDX() * factor);
            setAccumulatedDY(getAccumulatedDY() * factor);
        }
    }

    @Accessor("accumulatedDX")
    abstract double getAccumulatedDX();

    @Accessor("accumulatedDX")
    abstract void setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    abstract double getAccumulatedDY();

    @Accessor("accumulatedDY")
    abstract void setAccumulatedDY(double value);
}
