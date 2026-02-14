package com.spyglasshud.mixin;

import com.spyglasshud.SpyglassKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Options.class)
public class KeyBindingMixin {

    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addKeyBindings(CallbackInfo ci) {
        KeyMapping[] extended = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        extended[extended.length - 1] = SpyglassKeyBinding.OPEN_CONFIG;
        keyMappings = extended;
    }
}
