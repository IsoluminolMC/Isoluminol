package me.slightlke.isoluminol.config;

import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;

public final class ConfigManager {

    private static ConfigLoader<IsoluminolConfig> loader;
    private static IsoluminolConfig instance;

    public static void init() {
        loader = new ConfigLoader<>(Path.of("isoluminol.yml"), IsoluminolConfig.class);
        try {
            instance = loader.load();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    public static IsoluminolConfig get() {
        return instance;
    }
}
