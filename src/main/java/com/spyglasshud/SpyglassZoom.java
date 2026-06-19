package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 50.0;
    public static final double DEFAULT_ZOOM = 10.0;

    private static volatile double targetZoom = DEFAULT_ZOOM;
    private static volatile double currentZoom = DEFAULT_ZOOM;
    private static volatile double prevZoom = DEFAULT_ZOOM;
    private static volatile boolean wasScoping = false;

    /**
     * Per-frame smoothed zoom: interpolates between the previous and current
     * tick values by partialTick so the zoom is smooth above 20 FPS.
     */
    public static double get(float partialTick) {
        return prevZoom + (currentZoom - prevZoom) * partialTick;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     * Moves the target; the displayed zoom eases toward it each tick.
     */
    public static void adjust(double delta) {
        double step = SpyglassConfig.get().getZoomSensitivity();
        targetZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, targetZoom + Math.signum(delta) * step));
    }

    /**
     * Called every tick by ClientTickMixin.
     * Eases currentZoom toward targetZoom; resets all zoom state when the
     * player stops scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            targetZoom = DEFAULT_ZOOM;
            currentZoom = DEFAULT_ZOOM;
            prevZoom = DEFAULT_ZOOM;
        } else if (isScoping) {
            double smoothness = Math.min(1.0, Math.max(0.05, SpyglassConfig.get().getZoomSmoothness()));
            prevZoom = currentZoom;
            currentZoom += (targetZoom - currentZoom) * smoothness;
        }
        wasScoping = isScoping;
    }
}
