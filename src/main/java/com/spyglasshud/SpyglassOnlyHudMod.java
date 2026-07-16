package com.spyglasshud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class SpyglassOnlyHudMod implements ClientModInitializer {
    public static final String MOD_ID = "spyglass-only-hud";

    @Override
    public void onInitializeClient() {
        SpyglassConfig.load();
        KeyBindingHelper.registerKeyBinding(SpyglassKeyBinding.OPEN_CONFIG);
    }
}
