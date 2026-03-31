package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 20.0;
    public static final double DEFAULT_ZOOM = 10.0;
    private static final double STEP = 1.0;
    // Fraction of the gap closed per tick (20 ticks/s). 0.3 → ~97% after 10 ticks (0.5s).
    private static final double LERP_FACTOR = 0.3;

    private static volatile double targetZoom = DEFAULT_ZOOM;
    private static volatile double prevZoom = DEFAULT_ZOOM;    // snapshot at tick start, for partial-tick lerp
    private static volatile double currentZoom = DEFAULT_ZOOM; // smoothly follows targetZoom
    private static volatile boolean wasScoping = false;

    /**
     * Returns the interpolated zoom for the current render frame.
     * partialTick is in [0, 1] and blends prevZoom → currentZoom within a tick.
     */
    public static double get(float partialTick) {
        return prevZoom + (currentZoom - prevZoom) * partialTick;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     */
    public static void adjust(double delta) {
        targetZoom = Math.clamp(targetZoom + Math.signum(delta) * STEP, MIN_ZOOM, MAX_ZOOM);
    }

    /**
     * Called every tick by ClientTickMixin.
     * Resets zoom instantly when player stops scoping; lerps toward target while scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            targetZoom = DEFAULT_ZOOM;
            currentZoom = DEFAULT_ZOOM;
            prevZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
        if (isScoping) {
            prevZoom = currentZoom;
            currentZoom += (targetZoom - currentZoom) * LERP_FACTOR;
        }
    }
}
