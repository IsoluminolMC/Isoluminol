package me.slightlke.isoluminol.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNodeIntermediary;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigLoader<T> {

    private static final String HEADER = "Isoluminol configuration";
    private static final String PLACEHOLDER = "No configurable options yet. This file is a placeholder and does not need to be edited.";

    private final Path path;
    private final Class<T> configClass;
    private final ConfigurationOptions options;
    private final YamlConfigurationLoader loader;

    public ConfigLoader(final Path path, final Class<T> configClass) {
        this.path = path;
        this.configClass = configClass;
        this.loader = YamlConfigurationLoader.builder()
            .path(path)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(opts -> opts
                .serializers(builder -> builder.registerAnnotatedObjects(ObjectMapper.factory())))
            .build();
        this.options = this.loader.defaultOptions();
    }

    public T load() throws ConfigurateException {
        if (Files.notExists(this.path)) {
            return this.createDefault();
        }

        final ConfigurationNode node = this.loader.load();
        final T instance = node.get(this.configClass);
        if (instance == null) {
            throw new ConfigurateException("无法解析配置文件 " + this.path + "，请检查 YAML 格式");
        }
        return instance;
    }

    private T createDefault() throws ConfigurateException {
        try {
            if (this.path.getParent() != null) {
                Files.createDirectories(this.path.getParent());
            }

            final T instance = this.configClass.getDeclaredConstructor().newInstance();
            final CommentedConfigurationNode node = CommentedConfigurationNode.root(this.options);
            node.set(this.configClass, instance);
            this.saveAtomically(node);
            return instance;
        } catch (final ReflectiveOperationException e) {
            throw new ConfigurateException("无法创建配置对象 " + this.configClass.getName(), e);
        } catch (final IOException e) {
            throw new ConfigurateException("无法写入配置文件 " + this.path, e);
        }
    }

    private void saveAtomically(final CommentedConfigurationNode node) throws ConfigurateException, IOException {
        final Path parent = this.path.toAbsolutePath().getParent();
        final Path temp = Files.createTempFile(parent, this.path.getFileName().toString(), ".tmp");
        try {
            this.writeYaml(node, temp);

            try {
                Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void writeYaml(final ConfigurationNode node, final Path target) throws IOException {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setProcessComments(true);

        try (final Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            for (final String line : HEADER.split("\n")) {
                writer.write("# ");
                writer.write(line);
                writer.write('\n');
            }
            writer.write('\n');

            if (node.empty() || (node.isMap() && node.childrenMap().isEmpty())) {
                for (final String line : PLACEHOLDER.split("\n")) {
                    writer.write("# ");
                    writer.write(line);
                    writer.write('\n');
                }
                return;
            }

            new Yaml(dumperOptions).serialize(toSnakeYamlNode(node, true), writer);
        }
    }

    private static Node toSnakeYamlNode(final ConfigurationNode node, final boolean root) {
        if (node.isMap()) {
            final MappingNode mapping = new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
            boolean first = true;
            for (final Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                final ScalarNode key = new ScalarNode(Tag.STR, String.valueOf(entry.getKey()), null, null, DumperOptions.ScalarStyle.PLAIN);
                if (root && !first) {
                    appendBlankLine(key);
                }
                final Node value = toSnakeYamlNode(entry.getValue(), false);
                appendComments(commentOf(entry.getValue()), value);
                mapping.getValue().add(new NodeTuple(key, value));
                first = false;
            }
            return mapping;
        }

        if (node.isList()) {
            final SequenceNode sequence = new SequenceNode(Tag.SEQ, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
            for (final ConfigurationNode child : node.childrenList()) {
                final Node item = toSnakeYamlNode(child, false);
                appendComments(commentOf(child), item);
                sequence.getValue().add(item);
            }
            return sequence;
        }

        final Object raw = node.rawScalar();
        return new ScalarNode(tagFor(raw), formatScalar(raw), null, null, scalarStyleFor(raw));
    }

    private static void appendBlankLine(final Node target) {
        final List<CommentLine> lines = target.getBlockComments() == null
            ? new ArrayList<>()
            : new ArrayList<>(target.getBlockComments());
        lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
        target.setBlockComments(lines);
    }

    private static void appendComments(final String comment, final Node target) {
        if (comment == null || comment.isEmpty()) {
            return;
        }

        final List<CommentLine> lines = target.getEndComments() == null
            ? new ArrayList<>()
            : new ArrayList<>(target.getEndComments());
        for (final String line : comment.split("\n")) {
            lines.add(new CommentLine(null, null, " " + line, CommentType.BLOCK));
        }
        target.setEndComments(lines);
    }

    private static String commentOf(final ConfigurationNode node) {
        return node instanceof final CommentedConfigurationNodeIntermediary<?> commented ? commented.comment() : null;
    }

    private static Tag tagFor(final Object value) {
        if (value == null) {
            return Tag.NULL;
        }
        if (value instanceof Boolean) {
            return Tag.BOOL;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long || value instanceof BigInteger) {
            return Tag.INT;
        }
        if (value instanceof Float || value instanceof Double) {
            return Tag.FLOAT;
        }
        return Tag.STR;
    }

    private static String formatScalar(final Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static DumperOptions.ScalarStyle scalarStyleFor(final Object value) {
        return value instanceof String ? DumperOptions.ScalarStyle.DOUBLE_QUOTED : DumperOptions.ScalarStyle.PLAIN;
    }

    public Path path() {
        return this.path;
    }
}
