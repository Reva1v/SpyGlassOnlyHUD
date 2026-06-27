package com.spyglasshud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class SpyglassOnlyHudMod implements ClientModInitializer {
    public static final String MOD_ID = "spyglass-only-hud";

    @Override
    public void onInitializeClient() {
        SpyglassConfig.load();
        KeyMappingHelper.registerKeyMapping(SpyglassKeyBinding.OPEN_CONFIG);
    }
}
