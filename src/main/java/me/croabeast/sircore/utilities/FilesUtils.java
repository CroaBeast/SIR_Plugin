package me.croabeast.sircore.utilities;

import com.google.common.collect.Lists;
import me.croabeast.sircore.Application;
import me.croabeast.sircore.objects.*;
import org.bukkit.configuration.file.*;

import java.util.*;

public class FilesUtils {

    private final Application main;
    private final Recorder recorder;

    protected HashMap<String, YMLFile> files = new HashMap<>();

    private final List<String> filesList =
            Lists.newArrayList("" +
                    "config", "lang", "messages", "announces",
                    "chat", "motd", "discord", "advances"
            );

    private int FILES = 0;

    public FilesUtils(Application main) {
        this.main = main;
        this.recorder = main.getRecorder();
    }

    public void loadFiles(boolean debug) {
        if (!files.isEmpty()) files.clear();
        long time = System.currentTimeMillis();
        if (debug) recorder.doRecord("&bLoading plugin's files...");

        filesList.forEach(this::addFile);
        filesList.forEach(s -> files.get(s).updateInitFile());

        if (debug) recorder.doRecord(
                "&7Loaded &e" + FILES + "&7 files in &e" +
                (System.currentTimeMillis() - time) + "&7 ms."
        );
    }

    private void addFile(String name) {
        files.put(name, new YMLFile(main, name));
        files.get(name).reloadFile();
        FILES++;
    }

    public YMLFile getObject(String name) {
        return files.get(name);
    }

    public FileConfiguration getFile(String name) {
        return getObject(name).getFile();
    }
}
