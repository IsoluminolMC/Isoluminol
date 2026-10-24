package me.slightlke.isoluminol.config;

import me.slightlke.isoluminol.optimizations.simd.SIMDInitializer;
import me.slightlke.isoluminol.protocols.appleskin.AppleSkinSync;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;

public final class ConfigManager {

    private static volatile IsoluminolConfig instance = new IsoluminolConfig();
    private static boolean initialized;

    private ConfigManager() {
    }

    public static synchronized void init(final Path serverDirectory) {
        if (initialized) {
            throw new IllegalStateException("Isoluminol 配置已经初始化过了");
        }

        final Path configPath = serverDirectory.resolve("IsoLuminol.yml");
        final IsoluminolConfig loaded;
        try {
            final ConfigLoader<IsoluminolConfig> loader = new ConfigLoader<>(configPath, IsoluminolConfig.class);
            loaded = loader.load();
        } catch (final ConfigurateException e) {
            throw new IllegalStateException("无法加载 Isoluminol 配置 " + configPath, e);
        }

        instance = loaded;
        initialized = true;

        // The config decides which features are enabled; features do not check it themselves.
        if (loaded.optimizations().simd().enabled()) {
            SIMDInitializer.init();
        }
        if (loaded.protocols().appleskin().enabled()) {
            AppleSkinSync.init(loaded.protocols().appleskin().syncTickInterval());
        }
    }

    public static IsoluminolConfig get() {
        return instance;
    }
}
