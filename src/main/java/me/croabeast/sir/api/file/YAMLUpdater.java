package me.croabeast.sir.api.file;

import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
@UtilityClass
public class YAMLUpdater {

    private final char SEP = '.';

    public void updateFrom(InputStream resource, File file, String... ignored) throws IOException {
        updateFrom(resource, file, Arrays.asList(ignored));
    }

    public void updateFrom(InputStream resource, File file, List<String> ignored) throws IOException {
        Preconditions.checkArgument(file.exists(), "The file doesn't exist!");

        FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        FileConfiguration curConfig = YamlConfiguration.loadConfiguration(file);

        Map<String, String> comments = parseComments(resource, def);
        Map<String, String> ignoredValues = parseIgnored(file, comments, ignored == null ? Collections.emptyList() : ignored);

        StringWriter writer = new StringWriter();
        write(def, curConfig, new BufferedWriter(writer), comments, ignoredValues);

        String value = writer.toString();
        Path filePath = file.toPath();

        if (!value.equals(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)))
            Files.write(filePath, value.getBytes(StandardCharsets.UTF_8));
    }

    void write(FileConfiguration def, FileConfiguration current, BufferedWriter writer, Map<String, String> comments, Map<String, String> ignored) throws IOException {
        FileConfiguration parser = new YamlConfiguration();

        for (String key : def.getKeys(true)) {
            String indents = getIndents(key);

            if (!ignored.isEmpty() &&
                    writeIgnoredIfExists(ignored, writer, key))
                continue;

            final String c = comments.get(key);
            if (c != null) {
                String temp = c.substring(0, c.length() - 1).replace("\n", "\n" + indents);
                writer.write(indents + temp + "\n");
            }

            Object value = current.get(key);

            if (value == null)
                value = def.get(key);

            String[] splitKey = key.split("[" + SEP + "]");
            String lastKey = splitKey[splitKey.length - 1];

            if (value instanceof ConfigurationSection) {
                writer.write(indents + lastKey + ":");

                if (!((ConfigurationSection) value).getKeys(false).isEmpty()) {
                    writer.write("\n");
                    continue;
                }

                writer.write(" {}\n");
                continue;
            }

            parser.set(lastKey, value);

            String s = parser.saveToString();
            s = s.substring(0, s.length() - 1).replace("\n", "\n" + indents);

            String toWrite = indents + s + "\n";
            parser.set(lastKey, null);

            writer.write(toWrite);
        }

        String extraComments = comments.get(null);
        if (extraComments != null) writer.write(extraComments);

        writer.close();
    }

    Map<String, String> parseComments(InputStream resource, FileConfiguration def) throws IOException {
        Objects.requireNonNull(def);
        Objects.requireNonNull(resource);

        List<String> keys = new ArrayList<>(def.getKeys(true));
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

        Map<String, String> comments = new LinkedHashMap<>();
        StringBuilder commentBuilder = new StringBuilder();

        KeyBuilder keyBuilder = new KeyBuilder(def, SEP);
        String validKey = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("-")) continue;

            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                commentBuilder.append(trimmedLine).append("\n");
                continue;
            }

            if (!line.startsWith(" ")) {
                keyBuilder.clear();
                validKey = trimmedLine;
            }

            keyBuilder.parseLine(trimmedLine, true);
            String key = keyBuilder.toString();

            if (commentBuilder.length() > 0) {
                comments.put(key, commentBuilder.toString());
                commentBuilder.setLength(0);
            }

            int nextKeyIndex = keys.indexOf(keyBuilder.toString()) + 1;
            if (nextKeyIndex >= keys.size()) continue;

            String nextKey = keys.get(nextKeyIndex);
            while (!keyBuilder.isEmpty() && !nextKey.startsWith(keyBuilder.toString()))
                keyBuilder.removeLastKey();

            if (keyBuilder.isEmpty()) keyBuilder.parseLine(validKey, false);
        }

        reader.close();

        if (commentBuilder.length() > 0)
            comments.put(null, commentBuilder.toString());

        return comments;
    }

    Map<String, String> parseIgnored(File file, Map<String, String> comments, List<String> ignored) throws IOException {
        Map<String, String> ignoredValues = new LinkedHashMap<>(ignored.size());

        final DumperOptions options = new DumperOptions();
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);

        Map<Object, Object> root = yaml.load(new FileReader(file));

        ignored.forEach(section -> {
            String[] split = section.split("[" + SEP + "]");
            String key = split[split.length - 1];
            Map<Object, Object> map = getSection(section, root);

            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i != split.length - 1) {
                    if (keyBuilder.length() > 0)
                        keyBuilder.append(SEP);

                    keyBuilder.append(split[i]);
                }
            }

            ignoredValues.put(section, buildIgnored(key, map, comments, keyBuilder, new StringBuilder(), yaml));
        });

        return ignoredValues;
    }

    Map<Object, Object> getSection(String full, Map<Object, Object> root) {
        String[] keys = full.split("[" + SEP + "]", 2);
        String key = keys[0];

        Object value = root.get(getKeyAsObject(key, root));

        if (keys.length == 1) {
            if (value instanceof Map) return root;
            throw new IllegalArgumentException("Ignored sections must be a ConfigurationSection not a value!");
        }

        if (!(value instanceof Map))
            throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified!");

        return getSection(keys[1], (Map<Object, Object>) value);
    }

    String buildIgnored(String full, Map<Object, Object> map, Map<String, String> comments, StringBuilder builder, StringBuilder ignored, Yaml yaml) {
        String[] keys = full.split("[" + SEP + "]", 2);
        String key = keys[0];
        Object originalKey = getKeyAsObject(key, map);

        if (builder.length() > 0)
            builder.append(".");

        builder.append(key);

        if (!map.containsKey(originalKey)) {
            if (keys.length == 1)
                throw new IllegalArgumentException("Invalid ignored section: " + builder);

            throw new IllegalArgumentException("Invalid ignored section: " + builder + "." + keys[1]);
        }

        String comment = comments.get(builder.toString());
        String indents = getIndents(builder.toString());

        if (comment != null)
            ignored.append(addIndentation(comment, indents)).append("\n");

        ignored.append(addIndentation(key, indents)).append(":");
        Object obj = map.get(originalKey);

        if (obj instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) obj;
            if (m.isEmpty()) {
                ignored.append(" {}\n");
            } else {
                ignored.append("\n");
            }

            StringBuilder preLoopKey = new StringBuilder(builder);

            for (Object o : m.keySet()) {
                buildIgnored(o.toString(), m, comments, builder, ignored, yaml);
                builder = new StringBuilder(preLoopKey);
            }
        }
        else {
            String yml = yaml.dump(obj);

            if (obj instanceof Collection)
                ignored.append("\n").append(addIndentation(yml, indents)).append("\n");
            else ignored.append(" ").append(yml);
        }

        return ignored.toString();
    }

    String addIndentation(String s, String indents) {
        StringBuilder builder = new StringBuilder();
        String[] split = s.split("\n");

        for (String value : split) {
            if (builder.length() > 0)
                builder.append("\n");

            builder.append(indents).append(value);
        }

        return builder.toString();
    }

    Object getKeyAsObject(String key, Map<Object, Object> map) {
        if (map.containsKey(key)) return key;

        try {
            Float keyFloat = Float.parseFloat(key);
            if (map.containsKey(keyFloat))
                return keyFloat;
        } catch (NumberFormatException ignored) {}

        try {
            Double keyDouble = Double.parseDouble(key);
            if (map.containsKey(keyDouble))
                return keyDouble;
        } catch (NumberFormatException ignored) {}

        try {
            Integer keyInteger = Integer.parseInt(key);
            if (map.containsKey(keyInteger))
                return keyInteger;
        } catch (NumberFormatException ignored) {}

        try {
            Long longKey = Long.parseLong(key);
            if (map.containsKey(longKey))
                return longKey;
        } catch (NumberFormatException ignored) {}

        return null;
    }

    boolean writeIgnoredIfExists(Map<String, String> ignoredValues, BufferedWriter writer, String key) throws IOException {
        String ignored = ignoredValues.get(key);
        if (ignored != null) {
            writer.write(ignored);
            return true;
        }

        for (Map.Entry<String, String> entry : ignoredValues.entrySet())
            if (isSubKeyOf(entry.getKey(), key)) return true;

        return false;
    }

    boolean isSubKeyOf(String parentKey, String subKey) {
        return !parentKey.isEmpty() && (subKey.startsWith(parentKey) && subKey.substring(parentKey.length()).startsWith(String.valueOf(SEP)));
    }

    String getIndents(String key) {
        String[] splitKey = key.split("[" + SEP + "]");
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < splitKey.length; i++)
            builder.append("  ");

        return builder.toString();
    }

    static class KeyBuilder implements Cloneable {

        private final FileConfiguration config;
        private final char separator;
        private final StringBuilder builder;

        private KeyBuilder(FileConfiguration config, char separator) {
            this.config = config;
            this.separator = separator;
            this.builder = new StringBuilder();
        }

        private KeyBuilder(KeyBuilder keyBuilder) {
            this.config = keyBuilder.config;
            this.separator = keyBuilder.separator;
            this.builder = new StringBuilder(keyBuilder.toString());
        }

        public void parseLine(String line, boolean checkIfExists) {
            line = line.trim();

            String[] splitLine = line.split(":");

            if (splitLine.length > 2)
                splitLine = line.split(": ");

            String key = splitLine[0].replace("'", "").replace("\"", "");

            if (checkIfExists) {
                while (builder.length() > 0 && !config.contains(builder.toString() + separator + key)) {
                    removeLastKey();
                }
            }

            if (builder.length() > 0)
                builder.append(separator);

            builder.append(key);
        }

        public boolean isEmpty() {
            return builder.length() == 0;
        }

        public void clear() {
            builder.setLength(0);
        }

        public void removeLastKey() {
            if (builder.length() == 0)
                return;

            String keyString = builder.toString();
            String[] split = keyString.split("[" + separator + "]");
            int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
            builder.replace(minIndex, builder.length(), "");
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        @SuppressWarnings("all")
        @Override
        protected KeyBuilder clone() {
            return new KeyBuilder(this);
        }
    }
}
