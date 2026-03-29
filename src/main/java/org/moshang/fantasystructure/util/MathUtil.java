package org.moshang.fantasystructure.util;

public class MathUtil {
    public static final float PHI = 1.618033988749895f;
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float[] colorToFloat3D(int color) {
        return new float[] {
                ((color >> 16) & 0xFF) / 255.f,
                ((color >> 8) & 0xFF) / 255.f,
                (color & 0xFF) / 255.f
        };
    }

    public static float[] colorToFloat4D(int color) {
        return new float[] {
                ((color >> 16) & 0xFF) / 255.f,
                ((color >> 8) & 0xFF) / 255.f,
                (color & 0xFF) / 255.f,
                ((color >> 24) & 0xFF) / 255.f
        };
    }
}
