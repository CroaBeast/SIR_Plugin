package me.croabeast.sirplugin.objects;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.configuration.file.*;

import java.io.*;

/**
 * The class to handle a configuration file.
 */
public class YMLFile {

    /**
     * The instance of the plugin.
     */
    private final SIRPlugin main = SIRPlugin.getInstance();

    /**
     * The name of the file without its extension.
     */
    private final String name;

    /**
     * The location of the file with its extension.
     */
    private String location;

    /**
     * The file's folder. If the file is in a custom folder,
     * it will return its name, otherwise will be null.
     */
    private String folder = null;

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
    public YMLFile(String name) {
        this.name = name;
        this.location = name + ".yml";

        saveDefaultFile();
        reloadFile();
    }

    /**
     * File constructor with the file's name and its folder.
     * @param name the file's name without its extension.
     * @param folder the specified custom folder.
     */
    public YMLFile(String name, String folder) {
        this.name = name;
        this.location = name + ".yml";
        this.folder = folder;

        saveDefaultFile();
        reloadFile();
    }

    /**
     * Gets the raw file inside the plugin's folder.
     * @return the requested file.
     */
    private File catchFile() {
        if (folder != null) {
            File file = new File(main.getDataFolder(), folder);
            if (!file.exists()) file.mkdirs();
            return new File(file, location);
        }
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
     */
    public void saveFile() {
        if (this.file == null || this.rawYmlFile == null) return;

        try {
            this.getFile().save(this.catchFile());
        }
        catch (Exception e) {
            LogUtils.doLog("&7The &e" + location + "&7 file&c couldn't be saved&7.");
            e.printStackTrace();
        }

        LogUtils.doLog("&7The &e" + location + "&7 file has been&a saved&7.");
    }

    /**
     * Tries the file to change all the old values from older versions to newer versions.
     */
    private void updatingFile() {
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

        if (folder != null) location = folder + File.separator + location;
        LogUtils.doLog("&cFile " + location + " missing... &7Generating!");
        main.saveResource(location, false);
    }

    /**
     * Updates the file if its update is enabled.
     */
    public void updateInitFile() {
        if (main.getConfig().getStringList("updater.files").
                contains(name)) updatingFile();
        reloadFile();
    }
}
