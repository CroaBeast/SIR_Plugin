package me.croabeast.sircore.utils;

import me.croabeast.cupdater.ConfigUpdater;
import me.croabeast.sircore.Application;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Collections;

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
    }

    private File catchFile() { return new File(main.getDataFolder(), location); }

    public FileConfiguration getFile() { return file; }

    public void reloadFile() { file = YamlConfiguration.loadConfiguration(catchFile()); }

    private void saveFile() {
        if (file == null || rawYmlFile == null) return;
        try {
            this.getFile().save(this.rawYmlFile);
        } catch (IOException e) {
            main.logger("&6[SIR] &7The " + location + " file couldn't be saved...");
            e.printStackTrace();
        }
    }

    private void updatingFile() {
        try {
            ConfigUpdater.update(main, location, catchFile(), Collections.emptyList());
        } catch (IOException e) {
            main.logger("&6[SIR] &7The " + location + " file could not be updated...");
            e.printStackTrace();
        }
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
        main.logger("&6[SIR] &cFile " + location + " missing... &fGenerating!");
        saveDefaultFile();
    }

    public void updateInitFile() {
        if (main.getConfig().getBoolean("update." + name, true)) updatingFile();
        reloadFile();
    }
}
