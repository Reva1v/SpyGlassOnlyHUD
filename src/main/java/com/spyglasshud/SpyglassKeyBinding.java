package com.spyglasshud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class SpyglassKeyBinding {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("spyglass-only-hud", "settings"));

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "spyglass-only-hud.key.openConfig",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
    );
}
