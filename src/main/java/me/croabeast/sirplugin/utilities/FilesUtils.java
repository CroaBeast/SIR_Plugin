package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.objects.*;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.util.*;

public final class FilesUtils {

    HashMap<String, YMLFile> files = new HashMap<>();
    private int FILES = 0;

    public void loadFiles(boolean debug) {
        long time = System.currentTimeMillis();
        if (!files.isEmpty()) files.clear();

        if (debug)
            LogUtils.doLog("&bLoading plugin's files...");

        addFiles("config", "lang", "modules");

        addFiles((CharSequence) "chat", "emojis", "formats", "filters");
        addFiles((CharSequence) "messages",
                "advances", "announces", "join-quit");

        addFiles((CharSequence) "data", "ignore");
        addFiles((CharSequence) "misc", "discord", "motd");

        files.values().forEach(YMLFile::updateInitFile);
        files.values().forEach(YMLFile::reloadFile);

        if (debug)
            LogUtils.doLog(
                "&7Loaded &e" + FILES + "&7 files in &e" +
                (System.currentTimeMillis() - time) + "&7 ms."
            );
    }

    private void addFiles(String... names) {
        for (String name : names) {
            files.put(name, new YMLFile(name));
            files.get(name).reloadFile();
            FILES++;
        }
    }

    private void addFiles(CharSequence folder, String... names) {
        for (String name : names) {
            files.put(name, new YMLFile(name, folder.toString()));
            files.get(name).reloadFile();
            FILES++;
        }
    }

    @NotNull
    public YMLFile getObject(String name) {
        return files.get(name);
    }

    @NotNull
    public FileConfiguration getFile(String name) {
        return getObject(name).getFile();
    }
}
