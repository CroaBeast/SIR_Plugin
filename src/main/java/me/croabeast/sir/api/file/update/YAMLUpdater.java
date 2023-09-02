package me.croabeast.sir.api.file.update;

import me.croabeast.beanslib.utility.ArrayUtils;
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

public final class YAMLUpdater {

    private static final Charset UTF = StandardCharsets.UTF_8;

    private final InputStream resource;
    private final File toUpdate;
    private final List<String> ignored;

    private final FileConfiguration def, current;
    private final Map<String, String> comments, ignoredSections;

    private static <T> T checkNull(T obj) throws IOException {
        if (obj != null) return obj;
        throw new IOException("The input object is null.");
    }

    private YAMLUpdater(InputStream resource, File toUpdate, List<String> ignoredSections) throws IOException {
        this.resource = checkNull(resource);
        this.toUpdate = checkNull(toUpdate);

        this.ignored = ignoredSections == null ? new ArrayList<>() : ignoredSections;

        if (!this.toUpdate.exists())
            throw new IOException("The update file does not exist.");

        def = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, UTF));
        current = YamlConfiguration.loadConfiguration(toUpdate);

        Map<String, String> c = new HashMap<>();
        try {
            c = getComments();
        } catch (Exception ignored) {}

        Map<String, String> i = new HashMap<>();
        try {
            i = getIgnoredSections();
        } catch (Exception ignored) {}

        comments = c;
        this.ignoredSections = i;
    }

    private Map<String, String> getComments() throws IOException {
        List<String> keys = new ArrayList<>(def.getKeys(true));
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

        Map<String, String> comments = new LinkedHashMap<>();
        StringBuilder commentBuilder = new StringBuilder();

        KeyBuilder keyBuilder = new KeyBuilder(def, '.');
        String currentValidKey = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("-")) continue;

            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                if (!line.startsWith(" ")) {
                    keyBuilder.clear();
                    currentValidKey = trimmedLine;
                }

                keyBuilder.parseLine(trimmedLine, true);
                String key = keyBuilder.toString();

                if (commentBuilder.length() > 0) {
                    comments.put(key, commentBuilder.toString());
                    commentBuilder.setLength(0);
                }

                int nextKeyIndex = keys.indexOf(keyBuilder.toString()) + 1;

                if (nextKeyIndex < keys.size()) {
                    String nextKey = keys.get(nextKeyIndex);

                    while (!keyBuilder.isEmpty() && !nextKey.startsWith(keyBuilder.toString()))
                        keyBuilder.removeLastKey();

                    if (keyBuilder.isEmpty()) keyBuilder.parseLine(currentValidKey, false);
                }
            }
            else commentBuilder.append(trimmedLine).append("\n");
        }

        reader.close();

        if (commentBuilder.length() > 0) comments.put(null, commentBuilder.toString());
        return comments;
    }

    private static Object getKeyAsObject(String key, Map<Object, Object> sectionContext) {
        if (sectionContext.containsKey(key))
            return key;

        try {
            Float keyFloat = Float.parseFloat(key);

            if (sectionContext.containsKey(keyFloat))
                return keyFloat;

            Integer keyInteger = Integer.parseInt(key);

            if (sectionContext.containsKey(keyInteger))
                return keyInteger;

            Double keyDouble = Double.parseDouble(key);

            if (sectionContext.containsKey(keyDouble))
                return keyDouble;

            Long longKey = Long.parseLong(key);

            if (sectionContext.containsKey(longKey))
                return longKey;
        }
        catch (NumberFormatException ignored) {}

        return null;
    }

    @SuppressWarnings("all")
    private static Map<Object, Object> getSection(String fullKey, Map<Object, Object> root) {
        String[] keys = fullKey.split("[" + '.' + "]", 2);
        String key = keys[0];

        Object value = root.get(getKeyAsObject(key, root));
        if (keys.length == 1) {
            if (value instanceof Map) return root;
            throw new IllegalArgumentException("Ignored sections must be a ConfigurationSection.");
        }

        if (!(value instanceof Map))
            throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified.");

        return getSection(keys[1], (Map<Object, Object>) value);
    }

    private static String addIndentation(String s, String indents) {
        StringBuilder builder = new StringBuilder();
        String[] split = s.split("\n");

        for (String value : split) {
            if (builder.length() > 0) builder.append("\n");
            builder.append(indents).append(value);
        }

        return builder.toString();
    }

    @SuppressWarnings("all")
    private static String buildIgnored(
            String fullKey, Map<Object, Object> ymlMap, Map<String, String> comments,
            StringBuilder keyBuilder, StringBuilder igBuilder, Yaml yaml
    ) {
        String[] keys = fullKey.split("[" + '.' + "]", 2);
        String key = keys[0];
        Object originalKey = getKeyAsObject(key, ymlMap);

        if (keyBuilder.length() > 0) keyBuilder.append(".");
        keyBuilder.append(key);

        if (!ymlMap.containsKey(originalKey)) {
            String ex = "Invalid ignored section: " + keyBuilder;
            if (keys.length != 1) ex += "." + keys[1];

            throw new IllegalArgumentException(ex);
        }

        String indents = KeyUtils.getIndents(keyBuilder.toString());
        String comment = comments.get(keyBuilder.toString());

        igBuilder.
                append(addIndentation(comment != null ? comment : key, indents)).
                append(comment != null ? "\n" : ":");

        Object obj = ymlMap.get(originalKey);

        if (obj instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) obj;

            igBuilder.append((map.isEmpty() ? " {}" : "") + "\n");
            StringBuilder preLoopKey = new StringBuilder(keyBuilder);

            for (Object o : map.keySet()) {
                buildIgnored(o.toString(), map, comments, keyBuilder, igBuilder, yaml);
                keyBuilder = new StringBuilder(preLoopKey);
            }
        }
        else {
            boolean isCollection = obj instanceof Collection;
            String yml = yaml.dump(obj);

            igBuilder.append(isCollection ? "\n" : " ").append(
                    isCollection ?
                            addIndentation(yml, indents) :
                            yml
            );
            if (isCollection) igBuilder.append("\n");
        }

        return igBuilder.toString();
    }

    private Map<String, String> getIgnoredSections() throws IOException {
        Map<String, String> ignored = new LinkedHashMap<>(this.ignored.size());

        DumperOptions options = new DumperOptions();
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);
        Map<Object, Object> root = yaml.load(new FileReader(toUpdate));

        this.ignored.forEach(section -> {
            String[] split = section.split("[" + '.' + "]");
            String key = split[split.length - 1];

            Map<Object, Object> map = getSection(section, root);
            StringBuilder keyBuilder = new StringBuilder();

            for (int i = 0; i < split.length; i++) {
                if (i == split.length - 1) continue;

                if (keyBuilder.length() > 0) keyBuilder.append('.');
                keyBuilder.append(split[i]);
            }

            ignored.put(section, buildIgnored(key, map, comments, keyBuilder, new StringBuilder(), yaml));
        });

        return ignored;
    }

    private boolean writeIgnoredIfExists(BufferedWriter writer, String fullKey) throws IOException {
        String ignored = ignoredSections.get(fullKey);
        if (ignored != null) {
            writer.write(ignored);
            return true;
        }

        for (Map.Entry<String, String> entry : ignoredSections.entrySet())
            if (KeyUtils.isSubKeyOf(entry.getKey(), fullKey, '.')) return true;

        return false;
    }

    private void write(BufferedWriter writer) throws IOException {
        FileConfiguration parserConfig = new YamlConfiguration();

        for (String fullKey : def.getKeys(true)) {
            String indents = KeyUtils.getIndents(fullKey);

            if (!ignoredSections.isEmpty() &&
                    writeIgnoredIfExists(writer, fullKey)) continue;

            String comment = comments.get(fullKey);

            if (comment != null)
                writer.write(indents +
                        comment.substring(0, comment.length() - 1).
                        replace("\n", "\n" + indents) + "\n"
                );

            Object currentObj = current.get(fullKey);

            if (currentObj == null) currentObj = def.get(fullKey);

            String[] splitFullKey = fullKey.split("[" + '.' + "]");
            String trailingKey = splitFullKey[splitFullKey.length - 1];

            if (currentObj instanceof ConfigurationSection) {
                writer.write(indents + trailingKey + ":");

                if (!((ConfigurationSection) currentObj).getKeys(false).isEmpty()) {
                    writer.write("\n");
                } else {
                    writer.write(" {}\n");
                }
                continue;
            }

            parserConfig.set(trailingKey, currentObj);

            String yaml = parserConfig.saveToString();
            yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);

            final String toWrite = indents + yaml + "\n";
            parserConfig.set(trailingKey, null);

            writer.write(toWrite);
        }

        String danglingComments = comments.get(null);
        if (danglingComments != null) writer.write(danglingComments);

        writer.close();
    }

    void update() throws IOException {
        StringWriter writer = new StringWriter();
        write(new BufferedWriter(writer));

        String value = writer.toString();
        Path updatePath = toUpdate.toPath();

        byte[] bytes = Files.readAllBytes(updatePath);

        if (!value.equals(new String(bytes, UTF)))
            Files.write(updatePath, value.getBytes(UTF));
    }

    public static void updateFrom(InputStream resource, File toUpdate, List<String> ignored) throws IOException {
        new YAMLUpdater(resource, toUpdate, ignored).update();
    }

    @SuppressWarnings("all")
    public static void updateFrom(InputStream resource, File toUpdate, String... ignored) throws IOException {
        List<String> ignoreList = null;

        try {
            ignoreList = ArrayUtils.fromArray(null, ignored);
        } catch (Exception e) {}

        updateFrom(resource, toUpdate, ignoreList);
    }
}
