package me.croabeast.sir.api.file;

import lombok.AllArgsConstructor;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.Exceptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
public final class YAMLUpdater {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final char SEP = '.';

    private final FileLoader loader;
    private final String resourcePath;

    private final File file;

    private final FileConfiguration def;
    private final FileConfiguration current;

    private final Map<String, String> comments;
    private final Map<String, String> ignored;

    private <T> YAMLUpdater(T loader, String resourcePath, File file, List<String> ignored) throws IOException {
        this.loader = new FileLoader(loader);

        this.resourcePath = resourcePath;
        try {
            this.file = Exceptions.validate(f -> f != null && f.exists(), file);
        } catch (Exception e) {
            throw new IOException(e);
        }

        this.def = YamlConfiguration.loadConfiguration(fromReader());
        this.current = YamlConfiguration.loadConfiguration(file);

        this.comments = parseComments();
        this.ignored = parseIgnored(ignored == null ? Collections.emptyList() : ignored);
    }

    private Map<String, String> parseComments() throws IOException {
        List<String> keys = new ArrayList<>(def.getKeys(true));
        Map<String, String> comments = new LinkedHashMap<>();

        StringBuilder builder = new StringBuilder();
        KeyBuilder kb = new KeyBuilder(def, SEP);

        BufferedReader reader = new BufferedReader(fromReader());

        String currentValidKey = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String trim = line.trim();
            if (trim.startsWith("-")) continue;

            if (trim.isEmpty() || trim.startsWith("#")) {
                builder.append(trim).append("\n");
                continue;
            }

            if (!line.startsWith(" ")) {
                kb.clear();
                currentValidKey = trim;
            }

            kb.parseLine(trim, true);
            String key = kb.toString();

            if (builder.length() > 0) {
                comments.put(key, builder.toString());
                builder.setLength(0);
            }

            int index = keys.indexOf(kb.toString()) + 1;
            if (index >= keys.size()) continue;

            String next = keys.get(index);

            while (!kb.isEmpty() && !next.startsWith(kb.toString()))
                kb.removeLastKey();

            if (kb.isEmpty()) kb.parseLine(currentValidKey, false);
        }

        reader.close();
        if (builder.length() > 0) comments.put(null, builder.toString());

        return comments;
    }

    private InputStreamReader fromReader() {
        return new InputStreamReader(Objects.requireNonNull(loader.getResource(resourcePath)), UTF_8);
    }

    private Object getKeyAsObject(String key, Map<Object, Object> context) {
        if (context.containsKey(key)) return key;

        try {
            Float keyFloat = Float.parseFloat(key);
            if (context.containsKey(keyFloat))
                return keyFloat;
        } catch (NumberFormatException ignored) {}

        try {
            Double keyDouble = Double.parseDouble(key);
            if (context.containsKey(keyDouble))
                return keyDouble;
        } catch (NumberFormatException ignored) {}

        try {
            Integer keyInteger = Integer.parseInt(key);
            if (context.containsKey(keyInteger))
                return keyInteger;
        } catch (NumberFormatException ignored) {}

        try {
            Long longKey = Long.parseLong(key);
            if (context.containsKey(longKey))
                return longKey;
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private Map<Object, Object> getSection(String full, Map<Object, Object> root) {
        String[] keys = full.split("[" + SEP + "]", 2);
        String key = keys[0];

        Object value = root.get(getKeyAsObject(key, root));
        if (keys.length == 1) {
            if (value instanceof Map)
                return root;
            throw new IllegalArgumentException("Ignored sections must be a ConfigurationSection not a value!");
        }

        if (!(value instanceof Map))
            throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified!");

        return getSection(keys[1], (Map<Object, Object>) value);
    }

    private static String getIndents(final String key) {
        String[] splitKey = key.split("[" + SEP + "]");

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < splitKey.length; i++) builder.append("  ");

        return builder.toString();
    }

    private static String addIndent(String s, String indents) {
        StringBuilder builder = new StringBuilder();
        String[] split = s.split("\n");

        for (String value : split) {
            if (builder.length() > 0) builder.append("\n");
            builder.append(indents).append(value);
        }

        return builder.toString();
    }

    @AllArgsConstructor
    private class IgnoreBuilder {

        private final String full;
        private final Map<Object, Object> map;

        private StringBuilder builder;
        private final StringBuilder ignored;

        private final Yaml yaml;

        void writeIgnoredValue(Object o, String indents) {
            final String yml = yaml.dump(o);
            if (o instanceof Collection) {
                ignored.append("\n").append(addIndent(yml, indents)).append("\n");
                return;
            }

            ignored.append(" ").append(yml);
        }

        String build() {
            String[] keys = full.split("[" + SEP + "]", 2);
            String key = keys[0];
            Object originalKey = getKeyAsObject(key, map);

            if (builder.length() > 0) builder.append(".");
            builder.append(key);

            if (!map.containsKey(originalKey)) {
                String temp = "Invalid ignored section: " + builder;
                if (keys.length != 1)
                    temp += '.' + keys[1];

                throw new IllegalArgumentException(temp);
            }

            String comment = comments.get(builder.toString());
            String indents = getIndents(builder.toString());

            if (comment != null)
                ignored.append(addIndent(comment, indents)).append("\n");

            ignored.append(addIndent(key, indents)).append(":");

            Object obj = map.get(originalKey);
            if (obj instanceof Map) {
                Map<Object, Object> m = (Map<Object, Object>) obj;

                if (m.isEmpty()) ignored.append(" {}\n");
                else ignored.append("\n");

                StringBuilder preLoopKey = new StringBuilder(builder);

                for (Object o : m.keySet()) {
                    new IgnoreBuilder(o.toString(), m, builder, ignored, yaml).build();
                    builder = new StringBuilder(preLoopKey);
                }
            }
            else writeIgnoredValue(obj, indents);

            return ignored.toString();
        }
    }

    private Map<String, String> parseIgnored(List<String> sections) throws IOException {
        Map<String, String> values = new LinkedHashMap<>(sections.size());
        DumperOptions options = new DumperOptions();

        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);

        Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);
        Map<Object, Object> root = yaml.load(new FileReader(file));

        sections.forEach(section -> {
            String[] split = section.split("[" + SEP + "]");
            String key = split[split.length - 1];

            Map<Object, Object> map = getSection(section, root);

            StringBuilder b = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i == split.length - 1) continue;

                if (b.length() > 0) b.append(SEP);
                b.append(split[i]);
            }

            values.put(section, new IgnoreBuilder(key, map, b, new StringBuilder(), yaml).build());
        });

        return values;
    }

    static class KeyBuilder implements Cloneable {

        private final FileConfiguration c;
        private final char sep;
        private final StringBuilder b;

        public KeyBuilder(FileConfiguration c, char sep) {
            this.c = c;
            this.sep = sep;
            this.b = new StringBuilder();
        }

        private KeyBuilder(KeyBuilder key) {
            this.c = key.c;
            this.sep = key.sep;
            this.b = new StringBuilder(key.toString());
        }

        public void parseLine(String line, boolean checkIfExists) {
            String s = b.toString();
            line = line.trim();

            String[] current = line.split(":");

            if (current.length > 2)
                current = line.split(": ");

            String key = current[0].replace("'", "").replace("\"", "");

            if (checkIfExists) {
                while (b.length() > 0 && !c.contains(s + sep + key))
                    removeLastKey();
            }

            if (b.length() > 0) b.append(sep);
            b.append(key);
        }

        public void removeLastKey() {
            final int length = b.length();

            if (length != 0) {
                final String[] s = b.toString().split("[" + sep + "]");
                b.replace(Math.max(0, length - s[s.length - 1].length() - 1), length, "");
            }
        }

        public boolean isEmpty() {
            return b.length() == 0;
        }

        public void clear() {
            b.setLength(0);
        }

        @Override
        public String toString() {
            return b.toString();
        }

        @SuppressWarnings("all")
        protected KeyBuilder clone() {
            return new KeyBuilder(this);
        }
    }

    public void update() throws IOException {
        FileConfiguration parser = new YamlConfiguration();
        final StringWriter writer = new StringWriter();

        mainLoop: for (String full : def.getKeys(true)) {
            final String indents = getIndents(full);

            if (!ignored.isEmpty()) {
                final String ig = ignored.get(full);
                if (ig != null) {
                    writer.write(ig);
                    continue;
                }

                for (Map.Entry<String, String> entry : ignored.entrySet()) {
                    String key = entry.getKey();

                    if (!key.isEmpty() && (full.startsWith(key) &&
                            full.substring(key.length()).startsWith(SEP + "")))
                        continue mainLoop;
                }
            }

            String comment = comments.get(full);
            if (comment != null)
                writer.write(
                        indents + comment
                                .substring(0, comment.length() - 1)
                                .replace("\n", "\n" + indents)
                                + "\n");

            Object value = current.get(full);
            if (value == null) value = def.get(full);

            String[] split = full.split("[" + SEP + "]");
            String trailingKey = split[split.length - 1];

            if (value instanceof ConfigurationSection) {
                ConfigurationSection c = (ConfigurationSection) value;
                writer.write(indents + trailingKey + ":");

                String temp = "\n";
                if (c.getKeys(false).isEmpty()) temp = " {}" + temp;

                writer.write(temp);
                continue;
            }

            parser.set(trailingKey, value);

            String yaml = parser.saveToString();
            yaml = yaml
                    .substring(0, yaml.length() - 1)
                    .replace("\n", "\n" + indents);

            parser.set(trailingKey, null);
            writer.write(indents + yaml + "\n");
        }

        String danglingComments = comments.get(null);
        if (danglingComments != null) writer.write(danglingComments);

        writer.close();

        String value = writer.toString();
        Path path = file.toPath();

        if (!value.equals(new String(Files.readAllBytes(path), UTF_8)))
            Files.write(path, value.getBytes(UTF_8));
    }

    public static <T> YAMLUpdater of(T loader, String resourcePath, File file, List<String> ignored) throws IOException {
        return new YAMLUpdater(loader, resourcePath, file, ignored);
    }

    public static <T> YAMLUpdater of(T loader, String resourcePath, File file, String... ignored) throws IOException {
        return of(loader, resourcePath, file, ArrayUtils.toList(ignored));
    }
}
