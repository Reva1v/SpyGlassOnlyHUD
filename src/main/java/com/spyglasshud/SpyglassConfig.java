package com.spyglasshud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpyglassConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpyglassConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("spyglass-only-hud.json");

    private static SpyglassConfig instance;

    private boolean hideHud = true;
    private double overlayScale = 0.90;
    private boolean zoomEnabled = true;
    private double zoomSensitivity = 3.0;
    private double zoomSmoothness = 0.3;
    private boolean slowMouseWhileZooming = true;

    /** Never returns null: falls back to a default instance if nothing has been loaded yet. */
    public static SpyglassConfig get() {
        SpyglassConfig config = instance;
        if (config == null) {
            config = read();
            instance = config;
        }
        return config;
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
        boolean firstRun = !Files.exists(CONFIG_PATH);
        instance = read();
        if (firstRun) {
            save();
        }
    }

    /**
     * Reads the config file, falling back to defaults on a missing, empty or broken file.
     * Never returns null — {@link #get()} relies on that.
     */
    private static SpyglassConfig read() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SpyglassConfig loaded = GSON.fromJson(reader, SpyglassConfig.class);
                if (loaded != null) {
                    return loaded;
                }
                LOGGER.warn("Config file {} is empty, falling back to defaults", CONFIG_PATH);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Failed to read config file {}, falling back to defaults", CONFIG_PATH, e);
            }
        }
        return new SpyglassConfig();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(get(), writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write config file {}", CONFIG_PATH, e);
        }
    }
}
