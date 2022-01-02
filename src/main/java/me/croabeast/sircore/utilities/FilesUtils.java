package me.croabeast.sircore.utilities;

import me.croabeast.sircore.Application;
import me.croabeast.sircore.objects.YMLFile;
import org.bukkit.configuration.file.*;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FilesUtils {

    private final Application main;
    private final Recorder recorder;

    protected HashMap<String, YMLFile> files = new HashMap<>();
    protected List<String> filesList = new ArrayList<>();
    private int FILES = 0;

    public FilesUtils(Application main) {
        this.main = main;
        this.recorder = main.getRecorder();
    }

    public void loadFiles(boolean debug) {
        if (!files.isEmpty()) files.clear();
        long time = System.currentTimeMillis();
        if (debug) recorder.doRecord("&bLoading plugin's files...");

        registerFilesNames();
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

    private void registerFilesNames() {
        if (!filesList.isEmpty()) filesList.clear();

        Package mainPackage = main.getClass().getPackage();
        String name = mainPackage.getImplementationTitle();
        String vs = mainPackage.getImplementationVersion();

        JarFile jar;
        try {
            jar = new JarFile("" +
                    main.getDataFolder().getParentFile() +
                    File.separator + name + "-" + vs + ".jar"
            );
        }
        catch (Exception e) { return; }

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement().getName();
            if (entry.endsWith(".yml") && !entry.equals("plugin.yml"))
                filesList.add(entry.substring(0, entry.length() - 4));
        }

        Collections.swap(filesList, 0, filesList.indexOf("config"));
        Collections.swap(filesList, 1, filesList.indexOf("lang"));
    }

    public FileConfiguration getFile(String name) { return files.get(name).getFile(); }
}
