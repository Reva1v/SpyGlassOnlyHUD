package com.spyglasshud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpyglassConfigScreen extends Screen {

    private static final Component TITLE = Component.translatable("spyglass-only-hud.config.title");

    private static final int ROWS = 7;
    private static final int ROW_HEIGHT = 24;

    private final Screen lastScreen;
    private boolean hideHud;
    private double overlayScale;
    private boolean zoomEnabled;
    private double zoomSensitivity;
    private double zoomSmoothness; // interpolation factor, 0.05 (smoothest) .. 1.0 (instant)
    private boolean slowMouseWhileZooming;

    public SpyglassConfigScreen(Screen parent) {
        super(TITLE);
        this.lastScreen = parent;
        SpyglassConfig config = SpyglassConfig.get();
        this.hideHud = config.isHideHud();
        this.overlayScale = config.getOverlayScale();
        this.zoomEnabled = config.isZoomEnabled();
        this.zoomSensitivity = config.getZoomSensitivity();
        this.zoomSmoothness = config.getZoomSmoothness();
        this.slowMouseWhileZooming = config.isSlowMouseWhileZooming();
    }

    private int rowTop(int i) {
        return (this.height - ROWS * ROW_HEIGHT) / 2 + i * ROW_HEIGHT;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                buildHideHudLabel(),
                btn -> {
                    this.hideHud = !this.hideHud;
                    btn.setMessage(buildHideHudLabel());
                }
        ).bounds(this.width / 2 - 100, rowTop(0), 200, 20).build());

        this.addRenderableWidget(Button.builder(
                buildZoomEnabledLabel(),
                btn -> {
                    this.zoomEnabled = !this.zoomEnabled;
                    btn.setMessage(buildZoomEnabledLabel());
                }
        ).bounds(this.width / 2 - 100, rowTop(1), 200, 20).build());

        addStepperRow(2, () -> adjustScale(-0.05), () -> adjustScale(0.05));
        addStepperRow(3, () -> adjustSensitivity(-0.5), () -> adjustSensitivity(0.5));
        addStepperRow(4, () -> adjustSmoothness(0.05), () -> adjustSmoothness(-0.05));

        this.addRenderableWidget(Button.builder(
                buildSlowMouseLabel(),
                btn -> {
                    this.slowMouseWhileZooming = !this.slowMouseWhileZooming;
                    btn.setMessage(buildSlowMouseLabel());
                }
        ).bounds(this.width / 2 - 100, rowTop(5), 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> this.onClose()
        ).bounds(this.width / 2 - 100, rowTop(6), 200, 20).build());
    }

    private void addStepperRow(int row, Runnable onLeft, Runnable onRight) {
        this.addRenderableWidget(Button.builder(
                Component.literal("< "),
                btn -> onLeft.run()
        ).bounds(this.width / 2 - 100, rowTop(row), 20, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(" >"),
                btn -> onRight.run()
        ).bounds(this.width / 2 + 80, rowTop(row), 20, 20).build());
    }

    private void adjustScale(double delta) {
        this.overlayScale = Math.min(1.2, Math.max(0.5, this.overlayScale + delta));
    }

    private void adjustSensitivity(double delta) {
        this.zoomSensitivity = Math.min(10.0, Math.max(1.0, this.zoomSensitivity + delta));
    }

    private void adjustSmoothness(double delta) {
        // delta < 0 makes the factor smaller -> smoother; clamp to [0.05, 1.0]
        this.zoomSmoothness = Math.min(1.0, Math.max(0.05, this.zoomSmoothness + delta));
    }

    private int smoothnessPercent() {
        return (int) Math.round((1.0 - this.zoomSmoothness) / 0.95 * 100.0);
    }

    private Component buildHideHudLabel() {
        return Component.translatable("spyglass-only-hud.config.hideHud")
                .append(": ")
                .append(Component.translatable(this.hideHud ? "options.on" : "options.off"));
    }

    private Component buildZoomEnabledLabel() {
        return Component.translatable("spyglass-only-hud.config.zoomEnabled")
                .append(": ")
                .append(Component.translatable(this.zoomEnabled ? "options.on" : "options.off"));
    }

    private Component buildSlowMouseLabel() {
        return Component.translatable("spyglass-only-hud.config.slowMouseWhileZooming")
                .append(": ")
                .append(Component.translatable(this.slowMouseWhileZooming ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.drawCenteredString(this.font,
                Component.translatable("spyglass-only-hud.config.overlayScale")
                        .append(": " + Math.round(this.overlayScale * 100) + "%"),
                this.width / 2, rowTop(2) + 6, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("spyglass-only-hud.config.zoomSensitivity")
                        .append(": " + String.format("%.1f", this.zoomSensitivity)),
                this.width / 2, rowTop(3) + 6, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("spyglass-only-hud.config.zoomSmoothness")
                        .append(": " + smoothnessPercent() + "%"),
                this.width / 2, rowTop(4) + 6, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        SpyglassConfig config = SpyglassConfig.get();
        config.setHideHud(this.hideHud);
        config.setOverlayScale(this.overlayScale);
        config.setZoomEnabled(this.zoomEnabled);
        config.setZoomSensitivity(this.zoomSensitivity);
        config.setZoomSmoothness(this.zoomSmoothness);
        config.setSlowMouseWhileZooming(this.slowMouseWhileZooming);
        SpyglassConfig.save();
        this.minecraft.setScreen(this.lastScreen);
    }
}
