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

    /** Current tick-level zoom factor (no partial-tick smoothing). Used by mouse handling. */
    public static double getCurrent() {
        return currentZoom;
    }

    /**
     * Called by MouseHandlerMixin when the player scrolls while scoping.
     * delta > 0 = scroll up (zoom in), delta < 0 = scroll down (zoom out).
     * Moves the target; the displayed zoom eases toward it each tick.
     */
    public static void adjust(double delta) {
        // Multiplicative step: each scroll notch changes the zoom by a constant
        // percentage, so it feels the same at ×2 and at ×45 (like a real camera).
        // sensitivity 1..10 maps to +5%..+50% per notch.
        // Math.clamp is Java 21+, so clamp with min/max here (this branch targets Java 17).
        double factor = 1.0 + SpyglassConfig.get().getZoomSensitivity() * 0.05;
        if (delta > 0) {
            targetZoom *= factor;
        } else if (delta < 0) {
            targetZoom /= factor;
        }
        targetZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, targetZoom));
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
