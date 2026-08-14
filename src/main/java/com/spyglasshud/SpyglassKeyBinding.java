package com.spyglasshud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class SpyglassKeyBinding {
    public static final String CATEGORY = "key.category.spyglass-only-hud.settings";

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "spyglass-only-hud.key.openConfig",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
    );

    private SpyglassKeyBinding() {
    }
}
