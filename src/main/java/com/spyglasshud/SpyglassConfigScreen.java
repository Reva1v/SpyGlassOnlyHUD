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

        this.list.addBig(hideHudOption);
        this.list.addBig(overlayScaleOption);
    }

    @Override
    public void removed() {
        SpyglassConfig config = SpyglassConfig.get();
        config.setHideHud(hideHudOption.get());
        config.setOverlayScale(0.5 + overlayScaleOption.get() * 0.7);
        SpyglassConfig.save();
        super.removed();
    }
}
