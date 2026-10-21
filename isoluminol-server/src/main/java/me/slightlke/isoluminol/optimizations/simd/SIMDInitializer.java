package me.slightlke.isoluminol.optimizations.simd;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class SIMDInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SIMDInitializer() {
    }

    public static void init() {
        try {
            SIMDDetection.isEnabled = SIMDDetection.canEnable(LOGGER);
        } catch (final Throwable throwable) {
            SIMDDetection.isEnabled = false;
            LOGGER.warn("Failed to detect SIMD support", throwable);
        }

        if (SIMDDetection.isEnabled) {
            LOGGER.info("SIMD operations detected as functional. Map color conversion will use vectorized code.");
        } else {
            LOGGER.warn("SIMD operations are available for your server, but are not configured!");
            LOGGER.warn("To enable vectorized map color conversion, add \"--add-modules=jdk.incubator.vector\" before \"-jar\".");
        }
    }
}
