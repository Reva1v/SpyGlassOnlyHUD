package com.spyglasshud;

public class SpyglassZoom {
    public static final double MIN_ZOOM = 1.0;
    public static final double MAX_ZOOM = 50.0;
    public static final double DEFAULT_ZOOM = 10.0;
    private static final double STEP = 1.0;
    // Fixed zoom units moved per tick (20 ticks/s). 1.5 → 30 units/s, constant speed regardless of distance.
    // private static final double ZOOM_SPEED = 1.5;

    // private static volatile double targetZoom = DEFAULT_ZOOM;
    // private static volatile double prevZoom = DEFAULT_ZOOM;    // snapshot at tick start, for partial-tick lerp
    private static volatile double currentZoom = DEFAULT_ZOOM;
    private static volatile boolean wasScoping = false;

    /**
     * Returns the current zoom. partialTick reserved for smooth animation (currently unused).
     */
    public static double get(float partialTick) {
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
     * Resets zoom when player stops scoping.
     */
    public static void onTick(boolean isScoping) {
        if (wasScoping && !isScoping) {
            currentZoom = DEFAULT_ZOOM;
        }
        wasScoping = isScoping;
        // Smooth animation (disabled):
        // if (isScoping) {
        //     prevZoom = currentZoom;
        //     double diff = targetZoom - currentZoom;
        //     if (Math.abs(diff) <= ZOOM_SPEED) {
        //         currentZoom = targetZoom;
        //     } else {
        //         currentZoom += Math.signum(diff) * ZOOM_SPEED;
        //     }
        // }
    }
}
