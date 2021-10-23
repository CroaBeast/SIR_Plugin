package me.croabeast.sircore.objects;

import me.croabeast.cupdater.*;
import me.croabeast.sircore.*;
import org.bukkit.configuration.file.*;

import java.io.*;
import java.util.*;

public class SavedFile {

    private final Application main;
    private final String name;
    private final String location;
    private FileConfiguration file;
    private File rawYmlFile;

    public SavedFile(Application main, String name) {
        this.main = main;
        this.name = name;
        this.location = name + ".yml";
        registerFile();
        main.getInitializer().files++;
    }

    private File catchFile() { return new File(main.getDataFolder(), location); }

    public FileConfiguration getFile() { return file; }

    public void reloadFile() {
        if (name.equals("config")) main.reloadConfig();
        file = YamlConfiguration.loadConfiguration(catchFile());
    }

    private void saveFile() {
        if (file == null || rawYmlFile == null) return;
        try {
            this.getFile().save(this.rawYmlFile);
        } catch (IOException e) {
            main.doLogger("&7The &e" + location + "&7 file&c couldn't be saved&7.");
            e.printStackTrace();
        }
        main.doLogger("&7The &e" + location + "&7 file has been&a saved&7.");
    }

    private void updatingFile() {
        try {
            ConfigUpdater.update(main, location, catchFile(), Collections.emptyList());
        } catch (IOException e) {
            main.doLogger("&7The &e" + location + "&7 file&c couldn't be updated&7.");
            e.printStackTrace();
        }
        main.doLogger("&7The &e" + location + "&7 file has been&a updated&7.");
    }

    private void saveDefaultFile() {
        if (name.equals("config")) {
            main.saveDefaultConfig();
            return;
        }
        if (rawYmlFile == null) rawYmlFile = catchFile();
        if (!rawYmlFile.exists()) main.saveResource(location, false);
    }

    private void registerFile() {
        if (catchFile().exists()) return;
        main.doLogger("&6[SIR] &cFile " + location + " missing... &7Generating!");
        saveDefaultFile();
    }

    public void updateInitFile() {
        if (main.getConfig().getBoolean("updater.files." + name)) updatingFile();
        reloadFile();
    }
}
