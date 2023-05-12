package me.croabeast.sirplugin.file;

import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FileCache {

    private static final List<FileCache> CACHE_LIST = new ArrayList<>();
    private static final List<YMLFile> FILE_LIST = new ArrayList<>();

    private static boolean areFilesLoaded = false;

    public static final FileCache MAIN_CONFIG = new FileCache(null, "config");
    public static final FileCache LANG = new FileCache(null, "lang");
    public static final FileCache MODULES = new FileCache(null, "modules");

    public static final FileCache FILTERS = new FileCache("chat", "filters");
    public static final FileCache CHANNELS = new FileCache("chat", "channels");
    public static final FileCache EMOJIS = new FileCache("chat", "emojis");
    public static final FileCache MENTIONS = new FileCache("chat", "mentions");

    public static final FileCache IGNORE_DATA = new FileCache("data", "ignore");
    public static final FileCache CHAT_VIEW_DATA = new FileCache("data", "chat-view");

    public static final FileCache ADVANCE_CONFIG = new FileCache("advances", "config");
    public static final FileCache ADVANCE_LANG = new FileCache("advances", "lang");

    public static final FileCache ANNOUNCEMENTS = new FileCache("messages", "announces");
    public static final FileCache JOIN_QUIT = new FileCache("messages", "join-quit");

    public static final FileCache BOSSBARS_FILE = new FileCache("misc", "bossbars");
    public static final FileCache WEBHOOKS_FILE = new FileCache("misc", "webhooks");

    public static final FileCache MOTD_CACHE = new FileCache("misc", "motd");
    public static final FileCache DISCORD_CACHE = new FileCache("misc", "discord");

    private final String folder;
    private final String name;

    private FileCache(String folder, String name) {
        this.folder = folder;
        this.name = name;

        CACHE_LIST.add(this);
    }

    @Nullable
    public YMLFile getFile() {
        for (YMLFile f : FILE_LIST) if (f.equals(folder, name)) return f;
        return null;
    }

    public <T> T getValue(String path, T def) {
        YMLFile file = getFile();
        return file == null ? def : file.getValue(path, def);
    }

    @Nullable
    public <T> T getValue(String path, Class<T> clazz) {
        YMLFile file = getFile();
        return file == null ? null : file.getValue(path, clazz);
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        final YMLFile file = getFile();
        if (file == null) return null;

        final var f = file.get();
        return StringUtils.isBlank(path) ? f : f.getConfigurationSection(path);
    }

    public FileConfiguration get() {
        final YMLFile file = getFile();
        return file == null ? null : file.get();
    }

    public ConfigurationSection permSection(Player player, String path) {
        ConfigurationSection maxSection = null, id = get();
        String maxPerm = null, defKey = null;

        if (player == null) return null;

        if (StringUtils.isNotBlank(path) && id != null)
            id = id.getConfigurationSection(path);

        if (id == null) return null;

        var keys = id.getKeys(false);
        if (keys.isEmpty()) return null;

        int highestPriority = 0;
        boolean notDef = true;

        for (String k : keys) {
            var i = id.getConfigurationSection(k);
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

    public static void loadFiles() {
        final long time = System.currentTimeMillis();
        int totalFiles = 0, filesUpdates = 0;

        if (!areFilesLoaded) {
            for (FileCache f : CACHE_LIST) {
                YMLFile file = null;

                try {
                    file = new YMLFile(SIRPlugin.getInstance(), f.folder, f.name);
                } catch (Exception ignored) {}

                if (file == null) continue;
                FILE_LIST.add(file);

                var list = MAIN_CONFIG.toList("updater.files");
                if (file.getFolder() != null ||
                        !list.contains(file.getName())) continue;

                file.updateFile();
                filesUpdates++;
            }

            areFilesLoaded = true;
            LogUtils.doLog(
                    "&e" + FILE_LIST.size() + "&7 are loaded and &e" +
                            filesUpdates + "&7 of them were updated in &a" +
                            (System.currentTimeMillis() - time) + "&7 ms."
            );
            return;
        }

        for (YMLFile file : FILE_LIST) {
            file.reloadFile();
            totalFiles++;

            var list = MAIN_CONFIG.toList("updater.files");
            if (!list.contains(file + "")) continue;

            file.updateFile();
            filesUpdates++;
        }

        LogUtils.doLog(
                "&e" + totalFiles + "&7 were reloaded and &e" +
                        filesUpdates + "&7 of them were updated in &a" +
                        (System.currentTimeMillis() - time) + "&7 ms."
        );
    }
}
