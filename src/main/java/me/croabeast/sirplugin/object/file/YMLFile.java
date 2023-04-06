package me.croabeast.sirplugin.object.file;

import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.utility.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;

/**
 * The class to handle a configuration file.
 */
public class YMLFile {

    /**
     * The instance of the plugin.
     */
    private final JavaPlugin loader;

    /**
     * The name of the file without its extension.
     */
    @Getter
    private final String name;

    /**
     * The location of the file with its extension.
     */
    @Getter
    private final String location;

    /**
     * The folder denomination of the file, can be null.
     */
    @Getter
    private final String folder;

    /**
     * The configuration file used to get the values.
     */
    private FileConfiguration fc;

    /**
     * The raw file that is inside the specified location.
     */
    @Getter @NotNull
    private final File file;

    /**
     * File constructor with the file's name and its folder.
     *
     * @param loader the plugin's instance
     * @param folder the specified custom folder.
     * @param name the file's name without its extension.
     */
    public YMLFile(JavaPlugin loader, String folder, String name) {
        this.loader = loader;
        this.name = StringUtils.isBlank(name) ? ("file-" + hashCode()) : name;

        String location = name + ".yml";

        if (StringUtils.isNotBlank(folder)) {
            File file = new File(this.loader.getDataFolder(), folder);
            if (file.exists()) file.mkdirs();

            location = folder + File.separator + location;
        }

        this.folder = StringUtils.isBlank(folder) ? null : folder;
        this.location = location;

        this.file = new File(this.loader.getDataFolder(), location);

        saveDefaultFile();
        reloadFile();
    }

    /**
     * Reloads the file to update new edited values.
     *
     * @return the loaded configuration
     */
    @NotNull
    public FileConfiguration reloadFile() {
        return fc = YamlConfiguration.loadConfiguration(getFile());
    }

    /**
     * Returns the {@link FileConfiguration} instance of the .yml file.
     *
     * @return the requested configuration
     */
    @NotNull
    public FileConfiguration get() {
        return fc == null ? reloadFile() : fc;
    }

    /**
     * Saves the file to update new set values. It will delete all the file's comments.
     *
     * @param doLog if you want to show output when the file is saved
     * @return true if the file was saved, false otherwise
     */
    public boolean saveFile(boolean doLog) {
        if (this.fc == null) return false;

        try {
            get().save(this.getFile());
            if (doLog)
                LogUtils.doLog("&7The &e" + location + "&7 file has been&a saved&7.");
            return true;
        }
        catch (Exception e) {
            if (doLog) {
                LogUtils.doLog("&7The &e" + location + "&7 file&c couldn't be saved&7.");
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Saves the file to update new set values. It will delete all the file's comments.
     *
     * @return true if the file was saved, false otherwise
     */
    public boolean saveFile() {
        return saveFile(true);
    }

    /**
     * Tries the file to change all the old values from older versions to newer versions.
     *
     * @return true if the file was updated, false otherwise
     */
    public boolean updateFile() {
        try {
            ConfigUpdater.update(loader, location, getFile());
            if (LibUtils.getMainVersion() < 13)
                ConfigUpdater.update(loader, location, getFile());

            LogUtils.doLog("&7The &e" + location + "&7 file has been&a updated&7.");
            return true;
        }
        catch (Exception e) {
            LogUtils.doLog("&7The &e" + location + "&7 file&c couldn't be updated&7.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Similar to Bukkit's saveDefaultConfig method. It will initialize the file.
     *
     * @return true if the file was saved as default or that file already exists, false otherwise
     */
    public boolean saveDefaultFile() {
        if (file.exists()) return true;

        try {
            LogUtils.doLog("&cFile " + location + " missing... &7Generating!");
            loader.saveResource(location, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String path, T def) {
        return (T) get().get(path, def);
    }

    @Nullable
    public <T> T getValue(String path, Class<T> clazz) {
        try {
            return clazz.cast(get().get(path));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        return get().getConfigurationSection(path);
    }

    public boolean equals(String folder, String name) {
        return Objects.equals(this.folder, folder) && Objects.equals(this.name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (this == o) return true;
        if (getClass() != o.getClass()) return false;

        YMLFile f = (YMLFile) o;
        return equals(f.folder, f.name);
    }
}
