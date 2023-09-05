package me.croabeast.sir.plugin.file;

import lombok.Getter;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class FileCache implements CacheHandler {

    private static final List<FileCache> CACHE_LIST = new ArrayList<>();
    private static final List<YAMLFile> FILE_LIST = new ArrayList<>();

    private static boolean areFilesLoaded = false;

    public static final FileCache MAIN_CONFIG = new FileCache(null, "config");
    public static final FileCache BOSSBARS = new FileCache(null, "bossbars");
    public static final FileCache WEBHOOKS = new FileCache(null, "webhooks");

    private static final MultiFileCache LANG_CACHE = new MultiFileCache("lang", "lang-en", "lang-es");

    public static FileCache getLang() {
        return LANG_CACHE.getCache("lang-" + MAIN_CONFIG.getValue("lang", "en"));
    }

    public static final ModuleCache ADVANCE_CACHE = new ModuleCache("advancements", "lang", "messages");
    public static final ModuleCache ANNOUNCE_CACHE = new ModuleCache("announcements", "announces");

    public static final ModuleCache CHAT_CHANNELS_CACHE = new ModuleCache("chat_channels", "channels");
    public static final ModuleCache CHAT_COLORS_CACHE = new ModuleCache("chat_colors", "gui");
    public static final ModuleCache CHAT_TAGS_CACHE = new ModuleCache("chat_tags", "tags");

    public static final ModuleCache DISCORD_HOOK_CACHE = new ModuleCache("discord_hook", "channels");
    public static final ModuleCache JOIN_QUIT_CACHE = new ModuleCache("join_quit", "messages");
    public static final ModuleCache MOTD_CACHE = new ModuleCache("motd", "motds");

    public static final FileCache CHAT_FILTERS_CACHE = createUnaryModuleCache("chat_filters", "filters");
    public static final FileCache EMOJIS_CACHE = createUnaryModuleCache("emojis");
    public static final FileCache MENTIONS_CACHE = createUnaryModuleCache("mentions");

    public static final FileCache MODULES_DATA = new FileCache("data", "modules");
    public static final FileCache IGNORE_DATA = new FileCache("data", "ignore");
    public static final FileCache CHAT_VIEW_DATA = new FileCache("data", "chat-view");

    @Getter
    private final String folder, name;

    private FileCache(String folder, String name) {
        this.folder = folder;
        this.name = name;

        CACHE_LIST.add(this);
    }

    @Nullable
    public YAMLFile getFile() {
        for (YAMLFile f : FILE_LIST) if (f.equals(folder, name)) return f;
        return null;
    }

    public <T> T getValue(String path, T def) {
        return getFile() == null ? def : getFile().getValue(path, def);
    }

    @Nullable
    public <T> T getValue(String path, Class<T> clazz) {
        return getFile() == null ? null : getFile().getValue(path, clazz);
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        final YAMLFile file = getFile();
        if (file == null) return null;

        FileConfiguration f = file.get();
        return StringUtils.isBlank(path) ? f : f.getConfigurationSection(path);
    }

    @NotNull
    public List<String> getKeys(String path, boolean deep) {
        ConfigurationSection section = getSection(path);

        return section != null ?
                new ArrayList<>(section.getKeys(deep)) :
                new ArrayList<>();
    }

    public FileConfiguration get() {
        return getFile() == null ? null : getFile().get();
    }

    public ConfigurationSection permSection(Player player, String path) {
        ConfigurationSection maxSection = null, id = get();
        String maxPerm = null, defKey = null;

        if (player == null) return null;

        if (StringUtils.isNotBlank(path) && id != null)
            id = id.getConfigurationSection(path);

        if (id == null) return null;

        Set<String> keys = id.getKeys(false);
        if (keys.isEmpty()) return null;

        int highestPriority = 0;
        boolean notDef = true;

        for (String k : keys) {
            ConfigurationSection i = id.getConfigurationSection(k);
            if (i == null) continue;

            String perm = i.getString("permission", "DEFAULT");

            if (perm.matches("(?i)DEFAULT") && notDef) {
                defKey = k;
                notDef = false;
                continue;
            }

            int p = i.getInt("priority", perm.matches("(?i)DEFAULT") ? 0 : 1);

            if (PlayerUtils.hasPerm(player, perm) && p > highestPriority) {
                maxSection = i;
                maxPerm = perm;
                highestPriority = p;
            }
        }

        if (maxPerm != null && PlayerUtils.hasPerm(player, maxPerm))
            return maxSection;

        return defKey == null ? null : id.getConfigurationSection(defKey);
    }

    public List<String> toList(String path) {
        return TextUtils.toList(get(), path);
    }

    @Override
    public String toString() {
        if (getFile() == null) return "FileCache{}";
        return "FileCache{" + getFile().getLocation() + "}";
    }

    @Priority(level = 3)
    static void loadCache() {
        final long time = System.currentTimeMillis();
        int totalFiles = 0, filesUpdates = 0;

        if (!areFilesLoaded) {
            for (FileCache f : CACHE_LIST) {
                YAMLFile file;

                try {
                    file = new YAMLFile(SIRPlugin.getInstance(), f.folder, f.name) {
                        @Override
                        public boolean isUpdatable() {
                            return getValue("update", false);
                        }
                    };

                    String path = "resources" + File.separator + file.getLocation();
                    file.setResource(path.replace('\\', '/'));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                if (file.saveDefaults()) FILE_LIST.add(file);
                //if (file.update()) filesUpdates++;
            }

            areFilesLoaded = true;
            LogUtils.doLog(
                    "&e" + FILE_LIST.size() + "&7 are loaded and &e" +
                            filesUpdates + "&7 of them were updated in &a" +
                            (System.currentTimeMillis() - time) + "&7 ms."
            );
            return;
        }

        for (YAMLFile file : FILE_LIST) {
            file.reload();
            totalFiles++;

            //if (file.update()) filesUpdates++;
        }

        LogUtils.doLog(
                "&e" + totalFiles + "&7 were reloaded and &e" +
                        filesUpdates + "&7 of them were updated in &a" +
                        (System.currentTimeMillis() - time) + "&7 ms."
        );
    }

    private static FileCache createUnaryModuleCache(String folder, String name) {
        return new FileCache("modules" + File.separator + folder, name);
    }

    private static FileCache createUnaryModuleCache(String name) {
        return createUnaryModuleCache(name, name);
    }

    static class MultiFileCache {

        private final Map<String, FileCache> cacheMap = new HashMap<>();

        private MultiFileCache(String folder, List<String> files) {
            for (String s : files) {
                FileCache cache = new FileCache(folder, s);
                cacheMap.put(cache.name, cache);
            }
        }

        private MultiFileCache(String folder, String... files) {
            this(folder, Arrays.asList(files));
        }

        @NotNull
        public FileCache getCache(String file) {
            if (StringUtils.isBlank(file))
                throw new NullPointerException("File name is empty");

            List<FileCache> cache = new ArrayList<>();

            cacheMap.forEach((key, value) -> {
                if (file.equals(key)) cache.add(value);
            });

            if (cache.size() == 1) return cache.get(0);

            throw new NullPointerException("There is no file: " + file);
        }
    }

    private static List<String> moduleFiles(String... files) {
        List<String> list = new ArrayList<>();

        list.add("config");
        Collections.addAll(list, files);

        return list;
    }

    public static class ModuleCache extends MultiFileCache {

        private ModuleCache(String folder, String... files) {
            super("modules" + File.separator + folder, moduleFiles(files));
        }

        @NotNull
        public FileCache getConfig() {
            return getCache("config");
        }
    }
}
