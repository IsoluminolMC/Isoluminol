package me.slightlke.isoluminol.optimizations.simd;

import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Server-side replacement for {@link MapPalette#imageToBytes(Image)} that can
 * use the vectorized palette matcher.
 */
public final class SIMDMapPalette {

    private SIMDMapPalette() {
    }

    public static byte[] imageToBytes(final Image image) {
        final int width = image.getWidth(null);
        final int height = image.getHeight(null);
        final BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D graphics = temp.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        final int[] pixels = new int[width * height];
        temp.getRGB(0, 0, width, height, pixels, 0, width);

        final byte[] result = new byte[pixels.length];
        if (SIMDDetection.isEnabled) {
            VectorMapPalette.matchColorVectorized(pixels, result);
        } else {
            for (int i = 0; i < pixels.length; i++) {
                result[i] = MapPalette.matchColor(new Color(pixels[i], true));
            }
        }
        return result;
    }
}
