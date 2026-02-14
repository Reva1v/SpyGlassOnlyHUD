package com.spyglasshud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpyglassConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("spyglass-only-hud.json");

    private static SpyglassConfig instance;

    private boolean hideHud = true;
    private double overlayScale = 0.85;

    public static SpyglassConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public boolean isHideHud() {
        return hideHud;
    }

    public void setHideHud(boolean hideHud) {
        this.hideHud = hideHud;
    }

    public double getOverlayScale() {
        return overlayScale;
    }

    public void setOverlayScale(double overlayScale) {
        this.overlayScale = overlayScale;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, SpyglassConfig.class);
                if (instance == null) {
                    instance = new SpyglassConfig();
                }
            } catch (IOException e) {
                instance = new SpyglassConfig();
            }
        } else {
            instance = new SpyglassConfig();
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(get(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
