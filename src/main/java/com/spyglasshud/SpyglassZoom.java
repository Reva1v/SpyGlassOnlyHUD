package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 2.0;
    public static final double MAX_ZOOM = 15.0;
    public static final double DEFAULT_ZOOM = 10.0;
    private static final double STEP = 1.0;

    private static volatile double currentZoom = DEFAULT_ZOOM;
    private static volatile boolean wasScoping = false;

    public static double get() {
        return currentZoom;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     */
    public static void adjust(double delta) {
        currentZoom = Math.clamp(currentZoom + Math.signum(delta) * STEP, MIN_ZOOM, MAX_ZOOM);
    }

    /**
     * Called every tick by ClientTickMixin.
     * Resets zoom to DEFAULT_ZOOM when the player stops scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            currentZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
    }
}
