package com.spyglasshud;

import net.fabricmc.api.ClientModInitializer;

public class SpyglassOnlyHudMod implements ClientModInitializer {
    public static final String MOD_ID = "spyglass-only-hud";

    @Override
    public void onInitializeClient() {
        SpyglassConfig.load();
    }
}
