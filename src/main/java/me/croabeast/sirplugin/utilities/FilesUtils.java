package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.files.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static me.croabeast.sirplugin.utilities.FilesUtils.Folder.*;
import static me.croabeast.sirplugin.utilities.LogUtils.*;

public final class FilesUtils {

    private final SIRPlugin main;

    private final List<String> OLD_FILES =
            Arrays.asList("messages", "advances", "motd", "discord", "chat", "announces");

    HashMap<String, YMLFile> files = new HashMap<>();

    private int FILES = 0;
    private boolean notConversion = true;

    public FilesUtils(SIRPlugin main) {
        this.main = main;
    }

    @Nullable
    private File ymlFile(String name) {
        File file = new File(main.getDataFolder(), name + ".yml");
        return file.exists() ? file : null;
    }

    private boolean isUpdated() {
        for (String name : OLD_FILES) {
            File file = ymlFile(name);
            if (file != null && file.exists()) return false;
        }
        return true;
    }

    public void loadFiles(boolean debug) {
        long time = System.currentTimeMillis();
        if (!files.isEmpty()) files.clear();

        if (debug) LogUtils.doLog("&bLoading plugin's files...");

        boolean areMoved = false;
        File backUp = new File(main.getDataFolder(), "old-files");

        if (!isUpdated() && notConversion) {
            doLog("&eFound old files, moving to \"old-files\" folder...");
            if (!backUp.exists()) backUp.mkdirs();

            File[] fileArray = main.getDataFolder().listFiles();
            int i = 0;

            if (fileArray != null)
                for (File file : fileArray) {
                    if (file.getName().contains("old-files")) continue;
                    try {
                        file.renameTo(new File(backUp, file.getName()));
                        file.delete();
                        i++;
                    } catch (Exception e) {
                        doLog("&cCannot move " + file.getName() + ": &7" + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }

            doLog("&7Moved &e" + i + "&7 files to the \"old-files\" folder.", "");
            areMoved = true;
        }

        addFiles("config", "lang", "modules");

        addFiles(CHAT, "emojis", "formats", "filters", "mentions");
        addFiles(MESSAGES, "advances", "announces", "join-quit");

        addFiles(DATA, "ignore");
        addFiles(MISC, "discord", "motd");

        files.values().forEach(file -> {
            YMLFile config = getObject("config");
            if (config == null) return;

            List<String> list = config.getFile().getStringList("updater.files");
            if (list.contains(file + "")) file.updateFile();

            file.reloadFile();
        });

        files.values().forEach(YMLFile::reloadFile);

        if (areMoved) {
            doLog("", "&eTransferring old data to the new files...",
                    "&7The lang.yml and advances.yml files are gonna be skipped.");
            long t = System.currentTimeMillis();

            HashMap<String, YMLFile> oldFiles = new HashMap<>();

            int count = 0;
            File[] files = backUp.listFiles();

            if (files != null)
                for (File file : files) {
                    if (!file.getName().endsWith(".yml")) continue;

                    String name = file.getName().split("\\.")[0];
                    oldFiles.put(name, new YMLFile(main, name, "old-files"));

                    count++;
                }
            else doLog("&cFiles in the backup folder are null, cancelling...");

            if (count > 0) {
                boolean isConfig = false, isJoinQuit = false, isAnnounces = false,
                        isFormats = false, isMotd = false, isDiscord = false;

                int convertCount = 0;

                FileConfiguration oldConfig = oldFiles.get("config").getFile();
                if (oldConfig != null) {
                    ConfigurationSection updater = oldConfig.getConfigurationSection("updater.plugin");
                    if (updater != null) {
                        for (String key : updater.getKeys(false)) {
                            Object value = updater.get(key);
                            FileCache.CONFIG.get().set("updater.plugin." + key, value);
                        }
                    }

                    ConfigurationSection options = oldConfig.getConfigurationSection("options");
                    if (options != null) {
                        for (String key : options.getKeys(false)) {
                            Object value = options.get(key);
                            if (key.equals("format-logger")) key = "log-format";
                            FileCache.CONFIG.get().set("options." + key, value);
                        }
                    }

                    ConfigurationSection values = oldConfig.getConfigurationSection("values");
                    if (values != null) {
                        for (String key : values.getKeys(false)) {
                            Object value = values.get(key);
                            if (key.equals("config-prefix")) key = "lang-prefix-key";
                            FileCache.CONFIG.get().set("values." + key, value);
                        }
                    }

                    ConfigurationSection login = oldConfig.getConfigurationSection("login");
                    if (login != null) {
                        for (String key : login.getKeys(false)) {
                            Object value = login.get(key);
                            FileCache.MODULES.get().set("join-quit.login." + key, value);
                        }
                    }

                    ConfigurationSection vanish = oldConfig.getConfigurationSection("vanish");
                    if (vanish != null) {
                        for (String key : vanish.getKeys(false)) {
                            if (key.equals("silent")) continue;
                            Object value = vanish.get(key);
                            FileCache.MODULES.get().set("join-quit.vanish." + key, value);
                        }
                    }

                    ConfigurationSection chat = oldConfig.getConfigurationSection("chat");
                    if (chat != null) {
                        for (String key : chat.getKeys(false)) {
                            Object value = chat.get(key);
                            FileCache.MODULES.get().set("chat." + key, value);
                        }
                    }

                    ConfigurationSection advances = oldConfig.getConfigurationSection("advances");
                    if (advances != null) {
                        for (String key : advances.getKeys(false)) {
                            if (key.equals("enabled")) continue;
                            Object value = advances.get(key);
                            FileCache.MODULES.get().set("advancements." + key, value);
                        }
                    }

                    convertCount++;
                    isConfig = true;
                }

                FileConfiguration oldJoinQuit = oldFiles.get("messages").getFile();
                if (oldJoinQuit != null) {
                    for (String s : new String[] {"first-join", "join", "quit"}) {
                        ConfigurationSection section = oldJoinQuit.getConfigurationSection(s);
                        FileCache.JOIN_QUIT.get().set(s, section);
                    }

                    convertCount++;
                    isJoinQuit = true;
                }

                FileConfiguration oldAnnounces = oldFiles.get("announces").getFile();
                if (oldAnnounces != null) {
                    for (String key : new String[] {"interval", "random"}) {
                        Object value = oldAnnounces.get(key);
                        FileCache.MODULES.get().set("announces." + key, value);
                    }

                    ConfigurationSection s = oldAnnounces.getConfigurationSection("messages");
                    if (s != null) FileCache.ANNOUNCES.get().set("announces", s);

                    convertCount++;
                    isAnnounces = true;
                }

                FileConfiguration oldFormats = oldFiles.get("chat").getFile();
                if (oldFormats != null) {
                    FileCache.FORMATS.get().set("formats", oldFormats.getConfigurationSection("formats"));
                    convertCount++;
                    isFormats = true;
                }

                FileConfiguration oldMotd = oldFiles.get("motd").getFile();
                if (oldMotd != null) {
                    FileCache.MOTD.get().set("motds", oldMotd.getConfigurationSection("motd-list"));

                    Object value = oldMotd.getBoolean("random-motds");
                    FileCache.MODULES.get().set("motd.random-motds", value);

                    String[] keys = new String[] {"max-players.type", "max-players.count",
                            "server-icon.usage", "server-icon.image"};

                    for (String key : keys) {
                        Object value1 = oldMotd.get(key);
                        FileCache.MODULES.get().set("motd." + key, value1);
                    }

                    convertCount++;
                    isMotd = true;
                }

                FileConfiguration oldDiscord = oldFiles.get("discord").getFile();
                if (oldDiscord != null) {
                    Object server = oldDiscord.get("server-id");
                    FileCache.MODULES.get().set("discord.server-id", server);

                    ConfigurationSection ids = oldDiscord.getConfigurationSection("channels");
                    if (ids != null) {
                        for (String key : ids.getKeys(false)) {
                            Object value = ids.get(key);
                            FileCache.MODULES.get().set("discord.channels." + key, value);
                        }
                    }

                    FileCache.DISCORD.get().set("channels", oldDiscord.getConfigurationSection("formats"));

                    convertCount++;
                    isDiscord = true;
                }

                if (isConfig) FileCache.CONFIG.source().saveFile(false);
                if (isAnnounces) FileCache.ANNOUNCES.source().saveFile(false);
                if (isFormats) FileCache.FORMATS.source().saveFile(false);
                if (isJoinQuit) FileCache.JOIN_QUIT.source().saveFile(false);
                if (isMotd) FileCache.MOTD.source().saveFile(false);
                if (isDiscord) FileCache.DISCORD.source().saveFile(false);

                if (convertCount > 0) {
                    FileCache.MODULES.source().saveFile(false);
                    doLog("&7Transferred &e" + convertCount + "&7 files in &e"
                            + (System.currentTimeMillis() - t) + "&7 ms.", "");
                }
                else doLog("&cNo file was transferred, task cancelled.");
            }
            notConversion = false;
        }

        if (debug)
            LogUtils.doLog(
                "&7Loaded &e" + FILES + "&7 files in &e" +
                (System.currentTimeMillis() - time) + "&7 ms."
            );
    }

    private void addFiles(String... names) {
        for (String name : names) {
            files.put(name, new YMLFile(main, name));
            files.get(name).reloadFile();
            FILES++;
        }
    }

    private void addFiles(Folder folder, String... names) {
        for (String name : names) {
            files.put(name, new YMLFile(main, name, folder + ""));
            files.get(name).reloadFile();
            FILES++;
        }
    }

    @Nullable
    public YMLFile getObject(String name) {
        return files.get(name);
    }

    enum Folder {
        CHAT,
        MESSAGES,
        DATA,
        MISC;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
