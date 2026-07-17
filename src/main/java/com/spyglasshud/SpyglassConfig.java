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
    private double overlayScale = 0.90;
    private boolean zoomEnabled = true;
    private double zoomSensitivity = 3.0;
    private double zoomSmoothness = 0.3;
    private boolean slowMouseWhileZooming = true;

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

    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    public void setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
    }

    public double getZoomSensitivity() {
        return zoomSensitivity;
    }

    public void setZoomSensitivity(double zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    public double getZoomSmoothness() {
        return zoomSmoothness;
    }

    public void setZoomSmoothness(double zoomSmoothness) {
        this.zoomSmoothness = zoomSmoothness;
    }

    public boolean isSlowMouseWhileZooming() {
        return slowMouseWhileZooming;
    }

    public void setSlowMouseWhileZooming(boolean slowMouseWhileZooming) {
        this.slowMouseWhileZooming = slowMouseWhileZooming;
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
