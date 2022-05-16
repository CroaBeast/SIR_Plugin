package me.croabeast.sirplugin.objects;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

/**
 * The class to handle a configuration file.
 */
public class YMLFile {

    /**
     * The instance of the plugin.
     */
    private final JavaPlugin main;

    /**
     * The name of the file without its extension.
     */
    private final String name;

    /**
     * The location of the file with its extension.
     */
    private final String location;

    /**
     * The configuration file used to get the values.
     */
    private FileConfiguration file;

    /**
     * The raw file that is inside the specified location.
     */
    private File rawYmlFile;

    /**
     * Basic file constructor.
     * @param name the file's name without its extension.
     */
    public YMLFile(JavaPlugin main, String name) {
        this.main = main;
        this.name = name;
        this.location = name + ".yml";

        saveDefaultFile();
        file = YamlConfiguration.loadConfiguration(catchFile());
    }

    /**
     * File constructor with the file's name and its folder.
     * @param name the file's name without its extension.
     * @param folder the specified custom folder.
     */
    public YMLFile(JavaPlugin main, String name, String folder) {
        this.main = main;
        this.name = name;

        File folderFile = new File(main.getDataFolder(), folder);
        if (folderFile.exists()) folderFile.mkdirs();

        this.location = folder + File.separator + name + ".yml";

        saveDefaultFile();
        file = YamlConfiguration.loadConfiguration(catchFile());
    }

    /**
     * Gets the raw file inside the plugin's folder.
     * @return the requested file.
     */
    public File catchFile() {
        return new File(main.getDataFolder(), location);
    }

    /**
     * Gets the configuration file from its raw file.
     * @return the requested configuration.
     */
    public FileConfiguration getFile() {
        return file;
    }

    /**
     * Reloads the file to update new edited values.
     */
    public void reloadFile() {
        file = YamlConfiguration.loadConfiguration(catchFile());
    }

    /**
     * Saves the file to update new set values.
     * It will delete all the file's comments.
     * @param doLog if you want to show output when the file is saved
     */
    public void saveFile(boolean doLog) {
        if (this.file == null || this.rawYmlFile == null) return;

        try {
            this.getFile().save(this.catchFile());
        }
        catch (Exception e) {
            if (doLog) {
                LogUtils.doLog("&7The &e" + location + "&7 file&c couldn't be saved&7.");
                e.printStackTrace();
            }
        }

        if (doLog) LogUtils.doLog("&7The &e" + location + "&7 file has been&a saved&7.");
    }

    /**
     * Saves the file to update new set values.
     * It will delete all the file's comments.
     */
    public void saveFile() {
        saveFile(true);
    }

    /**
     * Tries the file to change all the old values from older versions to newer versions.
     */
    public void updateFile() {
        try {
            ConfigUpdater.update(main, location, catchFile());
            if (SIRPlugin.MAJOR_VERSION < 13)
                ConfigUpdater.update(main, location, catchFile());
        }
        catch (Exception e) {
            LogUtils.doLog("&7The &e" + location + "&7 file&c couldn't be updated&7.");
            e.printStackTrace();
        }
        LogUtils.doLog("&7The &e" + location + "&7 file has been&a updated&7.");
    }

    /**
     * Similar to Bukkit's saveDefaultConfig method. It will initialize the file.
     */
    public void saveDefaultFile() {
        if (rawYmlFile == null) rawYmlFile = catchFile();
        if (rawYmlFile.exists()) return;

        LogUtils.doLog("&cFile " + location + " missing... &7Generating!");
        main.saveResource(location, false);
    }

    @Override
    public String toString() {
        return name;
    }
}
