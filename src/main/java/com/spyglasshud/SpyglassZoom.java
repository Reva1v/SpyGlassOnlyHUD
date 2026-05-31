package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 50.0;
    public static final double DEFAULT_ZOOM = 10.0;
    private static double STEP = 3.0;

    private static volatile double currentZoom = DEFAULT_ZOOM;
    private static volatile boolean wasScoping = false;

    public static double get(float partialTick) {
        return currentZoom;
    }

    public static void adjust(double delta) {
        currentZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, currentZoom + Math.signum(delta) * STEP));
    }

    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            currentZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
    }
}
