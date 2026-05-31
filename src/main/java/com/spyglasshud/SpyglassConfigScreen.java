package com.spyglasshud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpyglassConfigScreen extends Screen {

    private static final Component TITLE = Component.translatable("spyglass-only-hud.config.title");

    private final Screen lastScreen;
    private boolean hideHud;
    private double overlayScale;

    public SpyglassConfigScreen(Screen parent) {
        super(TITLE);
        this.lastScreen = parent;
        SpyglassConfig config = SpyglassConfig.get();
        this.hideHud = config.isHideHud();
        this.overlayScale = config.getOverlayScale();
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                buildHideHudLabel(),
                btn -> {
                    this.hideHud = !this.hideHud;
                    btn.setMessage(buildHideHudLabel());
                }
        ).bounds(this.width / 2 - 100, this.height / 2 - 30, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("< "),
                btn -> adjustScale(-0.05)
        ).bounds(this.width / 2 - 100, this.height / 2 + 5, 20, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(" >"),
                btn -> adjustScale(0.05)
        ).bounds(this.width / 2 + 80, this.height / 2 + 5, 20, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> this.onClose()
        ).bounds(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build());
    }

    private void adjustScale(double delta) {
        this.overlayScale = Math.min(1.2, Math.max(0.5, this.overlayScale + delta));
    }

    private Component buildHideHudLabel() {
        return Component.translatable("spyglass-only-hud.config.hideHud")
                .append(": ")
                .append(Component.translatable(this.hideHud ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("spyglass-only-hud.config.overlayScale")
                        .append(": " + Math.round(this.overlayScale * 100) + "%"),
                this.width / 2, this.height / 2 - 8, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        SpyglassConfig config = SpyglassConfig.get();
        config.setHideHud(this.hideHud);
        config.setOverlayScale(this.overlayScale);
        SpyglassConfig.save();
        this.minecraft.setScreen(this.lastScreen);
    }
}
