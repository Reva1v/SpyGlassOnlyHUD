package com.spyglasshud;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class SpyglassConfigScreen extends OptionsSubScreen {

    private static final Component TITLE = Component.translatable("spyglass-only-hud.config.title");

    private OptionInstance<Boolean> hideHudOption;
    private OptionInstance<Double> overlayScaleOption;
    private OptionInstance<Boolean> zoomEnabledOption;
    private OptionInstance<Double> zoomSensitivityOption;
    private OptionInstance<Double> zoomSmoothnessOption;
    private OptionInstance<Boolean> slowMouseOption;

    public SpyglassConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, TITLE);
    }

    @Override
    protected void addOptions() {
        SpyglassConfig config = SpyglassConfig.get();

        hideHudOption = OptionInstance.createBoolean(
                "spyglass-only-hud.config.hideHud",
                config.isHideHud()
        );

        overlayScaleOption = new OptionInstance<>(
                "spyglass-only-hud.config.overlayScale",
                OptionInstance.noTooltip(),
                (caption, value) -> {
                    double actual = 0.5 + value * 0.7; // map 0.0-1.0 to 0.5-1.2
                    return Component.translatable("spyglass-only-hud.config.overlayScale")
                            .append(": " + Math.round(actual * 100) + "%");
                },
                OptionInstance.UnitDouble.INSTANCE,
                Codec.doubleRange(0.0, 1.0),
                (config.getOverlayScale() - 0.5) / 0.7, // map 0.5-1.2 to 0.0-1.0
                value -> {}
        );

        zoomEnabledOption = OptionInstance.createBoolean(
                "spyglass-only-hud.config.zoomEnabled",
                config.isZoomEnabled()
        );

        zoomSensitivityOption = new OptionInstance<>(
                "spyglass-only-hud.config.zoomSensitivity",
                OptionInstance.noTooltip(),
                (caption, value) -> {
                    double actual = 1.0 + value * 9.0; // map 0.0-1.0 to 1.0-10.0
                    return Component.translatable("spyglass-only-hud.config.zoomSensitivity")
                            .append(": " + String.format("%.1f", actual));
                },
                OptionInstance.UnitDouble.INSTANCE,
                Codec.doubleRange(0.0, 1.0),
                (config.getZoomSensitivity() - 1.0) / 9.0, // map 1.0-10.0 to 0.0-1.0
                value -> {}
        );

        zoomSmoothnessOption = new OptionInstance<>(
                "spyglass-only-hud.config.zoomSmoothness",
                OptionInstance.noTooltip(),
                (caption, value) ->
                        Component.translatable("spyglass-only-hud.config.zoomSmoothness")
                                .append(": " + Math.round(value * 100) + "%"),
                OptionInstance.UnitDouble.INSTANCE,
                Codec.doubleRange(0.0, 1.0),
                (1.0 - config.getZoomSmoothness()) / 0.95, // factor -> slider (higher = smoother)
                value -> {}
        );

        slowMouseOption = OptionInstance.createBoolean(
                "spyglass-only-hud.config.slowMouseWhileZooming",
                config.isSlowMouseWhileZooming()
        );

        this.list.addBig(hideHudOption);
        this.list.addBig(overlayScaleOption);
        this.list.addBig(zoomEnabledOption);
        this.list.addBig(zoomSensitivityOption);
        this.list.addBig(zoomSmoothnessOption);
        this.list.addBig(slowMouseOption);
    }

    @Override
    public void removed() {
        SpyglassConfig config = SpyglassConfig.get();
        config.setHideHud(hideHudOption.get());
        config.setOverlayScale(0.5 + overlayScaleOption.get() * 0.7);
        config.setZoomEnabled(zoomEnabledOption.get());
        config.setZoomSensitivity(1.0 + zoomSensitivityOption.get() * 9.0);
        config.setZoomSmoothness(1.0 - zoomSmoothnessOption.get() * 0.95);
        config.setSlowMouseWhileZooming(slowMouseOption.get());
        SpyglassConfig.save();
        super.removed();
    }
}
