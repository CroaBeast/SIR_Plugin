package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.utilities.FilesUtils.Folder.*;

public final class FilesUtils {

    private final SIRPlugin main;

    HashMap<String, YMLFile> files = new HashMap<>();
    private int FILES = 0;

    public FilesUtils(SIRPlugin main) {
        this.main = main;
    }

    public void loadFiles(boolean debug) {
        long time = System.currentTimeMillis();
        if (!files.isEmpty()) files.clear();

        if (debug)
            LogUtils.doLog("&bLoading plugin's files...");

        addFiles("config", "lang", "modules");

        addFiles(CHAT, "emojis", "formats", "filters");
        addFiles(MESSAGES,
                "advances", "announces", "join-quit");

        addFiles(DATA, "ignore");
        addFiles(MISC, "discord", "motd");

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
