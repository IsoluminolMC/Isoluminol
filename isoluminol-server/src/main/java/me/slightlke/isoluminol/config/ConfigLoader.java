package me.slightlke.isoluminol.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader<T> {

    private final Path path;
    private final Class<T> configClass;
    private final YamlConfigurationLoader loader;
    private T instance;

    public ConfigLoader(Path path, Class<T> configClass) {
        this.path = path;
        this.configClass = configClass;
        this.loader = YamlConfigurationLoader.builder()
            .path(path)
            .defaultOptions(opts -> opts.serializers(builder ->
                builder.registerAnnotatedObjects(ObjectMapper.factory())))
            .build();
    }

    public T load() throws ConfigurateException {
        boolean isNew = Files.notExists(path);

        if (isNew) {
            try {
                instance = configClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new ConfigurateException(e);
            }
            ConfigurationNode node = CommentedConfigurationNode.root(loader.defaultOptions());
            node.set(configClass, instance);
            loader.save(node);
            return instance;
        }

        ConfigurationNode node = loader.load();
        instance = node.get(configClass);
        if (instance == null) {
            throw new ConfigurateException();
        }
        return instance;
    }

    public T get() {
        return instance;
    }
}
