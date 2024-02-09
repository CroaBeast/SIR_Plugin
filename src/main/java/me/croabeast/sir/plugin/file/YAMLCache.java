package me.croabeast.sir.plugin.file;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@UtilityClass
public class YAMLCache implements CacheManageable {

    private String getKey(String folder, String name) {
        return StringUtils.isNotBlank(folder) ? (folder + ':' + name) : name;
    }

    static class YAMLMap {

        private final List<String> keys = new ArrayList<>();
        private boolean isMain = false;

        YAMLMap setMain() {
            this.isMain = true;
            return this;
        }
    }

    private final Set<YAMLMap> YAML_MAP_SET = new LinkedHashSet<>();

    YAMLMap of(String folder, String... names) {
        YAMLMap map = new YAMLMap();
        for (String s : names)
            map.keys.add(getKey(folder, s));

        YAML_MAP_SET.add(map);
        return map;
    }

    YAMLMap moduleFolder(ModuleName name, String... names) {
        String folder = name.getFolderPath();
        YAMLMap map = new YAMLMap();

        map.keys.add(getKey(folder, "config"));

        for (String s : names)
            map.keys.add(getKey(folder, s));

        YAML_MAP_SET.add(map);
        return map;
    }

    YAMLMap singleModule(ModuleName module, String name) {
        String folder = module.getFolderPath();

        YAMLMap map = new YAMLMap();
        map.keys.add(getKey(
                folder,
                StringUtils.isBlank(name) ?
                        module.getFolderName() :
                        name
        ));

        YAML_MAP_SET.add(map);
        return map;
    }

    static {
        of("", "config", "bossbars", "webhooks").setMain();
        of("lang", "lang-en", "lang-es", "lang-ru");

        moduleFolder(ModuleName.ADVANCEMENTS, "lang", "messages");
        moduleFolder(ModuleName.ANNOUNCEMENTS, "announces");

        moduleFolder(ModuleName.CHAT_CHANNELS, "channels");
        moduleFolder(ModuleName.JOIN_QUIT, "messages");
        moduleFolder(ModuleName.DISCORD_HOOK, "channels");
        moduleFolder(ModuleName.MOTD, "motds");

        singleModule(ModuleName.CHAT_FILTERS, "filters");
        singleModule(ModuleName.EMOJIS, null);
        singleModule(ModuleName.MENTIONS, null);

        of("data", "modules", "ignore", "chat-view", "commands", "mute");
    }

    private final Map<String, YAMLFile> FILE_MAP = new LinkedHashMap<>();
    private boolean areFilesLoaded = false;

    private static class SIRFile extends YAMLFile {

        SIRFile(String folder, String name) throws IOException {
            super(SIRPlugin.getInstance(), folder, name);
            setResource("resources" + File.separator + getLocation());
        }

        @Override
        public boolean isUpdatable() {
            return get("update", false);
        }
    }

    @Priority(3)
    void loadCache() {
        if (areFilesLoaded) return;

        long time = System.currentTimeMillis();
        int totalFiles = 0, filesUpdated = 0;

        for (YAMLMap map : YAML_MAP_SET)
            for (final String key : map.keys) {
                String folder = null;
                String name = key;

                if (!map.isMain) {
                    String[] array = key.split(":", 2);
                    folder = array[0];
                    name = array[1];
                }

                try {
                    YAMLFile file = new SIRFile(folder, name);
                    if (file.saveDefaults()) {
                        FILE_MAP.put(key, file);
                        totalFiles++;
                        // if (file.update()) filesUpdated++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        time = System.currentTimeMillis() - time;

        LogUtils.doLog("&e" + totalFiles +
                "&7 were loaded and &e" + filesUpdated +
                "&7 were updated in &a" + time + "&7 ms."
        );
        areFilesLoaded = true;
    }

    @Priority(3)
    void saveCache() {
        if (!areFilesLoaded) return;

        long time = System.currentTimeMillis();
        int totalFiles = 0, filesUpdated = 0;

        for (YAMLFile file : FILE_MAP.values()) {
            file.reload();
            totalFiles++;
            // if (file.update()) filesUpdated++;
        }

        time = System.currentTimeMillis() - time;

        LogUtils.doLog("&e" + totalFiles +
                "&7 were reloaded and &e" + filesUpdated +
                "&7 were updated in &a" + time + "&7 ms."
        );
    }

    public YAMLFile getMainConfig() {
        return FILE_MAP.get("config");
    }

    public YAMLFile getBossbars() {
        return FILE_MAP.get("bossbars");
    }

    public YAMLFile getWebhooks() {
        return FILE_MAP.get("webhooks");
    }

    public YAMLFile getLang() {
        return FILE_MAP.get("lang:lang-" + getMainConfig().get("lang", "en"));
    }

    public YAMLFile fromAdvances(String name) {
        return FILE_MAP.get(ModuleName.ADVANCEMENTS.getFolderPath() + ':' + name);
    }

    public YAMLFile fromAnnounces(String name) {
        return FILE_MAP.get(ModuleName.ANNOUNCEMENTS.getFolderPath() + ':' + name);
    }

    public YAMLFile fromChannels(String name) {
        return FILE_MAP.get(ModuleName.CHAT_CHANNELS.getFolderPath() + ':' + name);
    }

    public YAMLFile fromJoinQuit(String name) {
        return FILE_MAP.get(ModuleName.JOIN_QUIT.getFolderPath() + ':' + name);
    }

    public YAMLFile fromDiscordHook(String name) {
        return FILE_MAP.get(ModuleName.DISCORD_HOOK.getFolderPath() + ':' + name);
    }

    public YAMLFile fromMotd(String name) {
        return FILE_MAP.get(ModuleName.MOTD.getFolderPath() + ':' + name);
    }

    public YAMLFile getFilters() {
        return FILE_MAP.get(ModuleName.CHAT_FILTERS.getFolderPath() + ":filters");
    }

    public YAMLFile getEmojis() {
        return FILE_MAP.get(ModuleName.EMOJIS.getFolderPath() + ":emojis");
    }

    public YAMLFile getMentions() {
        return FILE_MAP.get(ModuleName.MENTIONS.getFolderPath() + ":mentions");
    }

    public YAMLFile fromData(String name) {
        return FILE_MAP.get("data:" + name);
    }
}
