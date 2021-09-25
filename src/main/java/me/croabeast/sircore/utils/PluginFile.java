package me.croabeast.sircore.utils;

import me.croabeast.cupdater.ConfigUpdater;
import me.croabeast.sircore.MainClass;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class PluginFile {

    private final MainClass main;
    private final String name;
    private final String location;
    private FileConfiguration custom;
    private File customFile;

    public PluginFile(MainClass main, String name) {
        this.main = main;
        this.name = name;
        this.location = name + ".yml";
        registerFile();
    }

    private File catchFile() { return new File(main.getDataFolder(), location); }

    public FileConfiguration getFile() {
        if (custom == null) reloadFile();
        return custom;
    }

    private void saveFile() {
        if (custom == null || customFile == null) return;
        try {
            this.getFile().save(this.customFile);
        } catch (IOException e) {
            main.logger("&6[SIR] &7The " + location + " file couldn't be saved...");
            e.printStackTrace();
        }
    }

    public void reloadFile() {
        if (name.equals("config")) { main.reloadConfig(); return; }
        if (customFile == null) customFile = catchFile();
        custom = YamlConfiguration.loadConfiguration(customFile);

        Reader stream; InputStream mainResource = main.getResource(location);
        if (mainResource != null) {
            stream = new InputStreamReader(mainResource, StandardCharsets.UTF_8);
            YamlConfiguration file = YamlConfiguration.loadConfiguration(stream);
            custom.setDefaults(file);
        }
    }

    private void updateFile() {
        try {
            ConfigUpdater.update(main, location, catchFile(), Collections.emptyList());
        } catch (IOException e) {
            main.logger("&6[SIR] &7The " + location + " file could not be updated...");
            e.printStackTrace();
        }
    }

    private void saveDefaultFile() {
        if (name.equals("config")) { main.saveDefaultConfig(); return; }
        if (customFile == null) customFile = catchFile();
        if (!customFile.exists()) main.saveResource(location, false);
    }

    private void registerFile() {
        if (catchFile().exists()) return;
        main.logger("&6[SIR] &cFile " + location + " missing... &fGenerating!");
        saveDefaultFile(); reloadFile();
    }

    public void updateRegisteredFile() {
        boolean setUp = main.getConfig().getBoolean("update." + name, true);
        if (setUp && !name.equals("messages")) updateFile(); reloadFile();
    }
}
