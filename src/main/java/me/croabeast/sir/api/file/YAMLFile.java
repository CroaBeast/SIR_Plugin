package me.croabeast.sir.api.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.beans.BeansLib;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.api.ResourceIOUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * The YAMLFile class provides functionality for loading, saving, and managing YAML configuration files.
 * It can load configuration data from plugin resources or existing files, and save changes back to disk.
 */
@Accessors(chain = true)
@Getter
public class YAMLFile {

    @Getter(AccessLevel.NONE)
    private final FileLoader loader;

    /**
     * The name of the YAML file.
     */
    private String name = "file-" + hashCode();
    /**
     * The folder within the data folder where the file is located.
     */
    @Nullable
    private String folder;

    /**
     * The location of the YAML file.
     */
    private final String location;
    /**
     * The File object representing the YAML file.
     */
    @NotNull
    private final File file;

    @Getter(AccessLevel.NONE)
    private String resourcePath;
    @Getter(AccessLevel.NONE)
    private FileConfiguration configuration;

    @Getter(AccessLevel.NONE)
    private YAMLUpdater updater;

    /**
     * Indicates whether the YAML file is updatable.
     */
    @Setter
    private boolean updatable = true;

    /**
     * Constructs a YAMLFile instance with the specified loader, folder, and name.
     *
     * @param loader The loader object associated with this YAML file.
     * @param folder The folder within the data folder where the file is located.
     * @param name   The name of the YAML file.
     * @throws IOException If an I/O error occurs while loading the file or resource.
     */
    public <T> YAMLFile(T loader, @Nullable String folder, String name) throws IOException {
        this.loader = new FileLoader(loader);

        if (StringUtils.isNotBlank(name))
            this.name = name;

        File dataFolder = this.loader.getDataFolder();
        String location = name + ".yml";

        if (StringUtils.isNotBlank(folder)) {
            this.folder = folder;

            File file = new File(dataFolder, folder);
            if (!file.exists()) file.mkdirs();

            location = folder + File.separator + location;
        }

        this.location = location;
        file = new File(dataFolder, location);

        try {
            setResourcePath(location);
        } catch (Exception ignored) {}

        try {
            this.updater = YAMLUpdater.of(loader, resourcePath, file);
        } catch (Exception ignored) {}
    }

    /**
     * Constructs a YAMLFile instance with the specified loader, and name.
     *
     * @param loader The loader object associated with this YAML file.
     * @param name   The name of the YAML file.
     * @throws IOException If an I/O error occurs while loading the file or resource.
     */
    public <T> YAMLFile(T loader, String name) throws IOException {
        this(loader, null, name);
    }

    private void loadUpdaterToData(boolean debug) {
        try {
            this.updater = YAMLUpdater.of(loader.getLoader(), this.resourcePath, getFile());
        } catch (Exception e) {
            if (debug) e.printStackTrace();
        }
    }

    public YAMLFile setResourcePath(String path, boolean debug) {
        resourcePath = Exceptions.validate(StringUtils::isNotBlank, path).replace('\\', '/');
        loadUpdaterToData(debug);
        return this;
    }

    public YAMLFile setResourcePath(String path) {
        return setResourcePath(path, false);
    }

    public InputStream getResource() {
        return loader.getResource(resourcePath);
    }

    /**
     * Reloads the FileConfiguration object from the YAML file.
     *
     * @return The reloaded FileConfiguration object.
     */
    @NotNull
    public FileConfiguration reload() {
        return configuration = YamlConfiguration.loadConfiguration(getFile());
    }

    private void log(String line, boolean debug) {
        if (debug) BeansLib.logger().log(line);
    }

    private void log(String line, Exception e, boolean debug) {
        log(line + " (&c" + e.getLocalizedMessage() + "&7)", debug);
    }

    /**
     * Saves default configuration settings to the YAML file if it does not exist already.
     * <ul>
     *     <li> If the file exists, it returns true immediately.</li>
     *     <li> IIf the file does not exist, it attempts to save default settings from the resource file.</li>
     *     <li> If saving from the resource file fails, it logs an error and returns false.</li>
     *     <li> If the file is successfully generated from the resource, it reloads the configuration.</li>
     * </ul>
     *
     * @return True if the file already exists or if default settings were successfully saved from the resource file, false otherwise.
     */
    public boolean saveDefaults(boolean debug) {
        if (getFile().exists()) return true;

        try {
            ResourceIOUtils.saveResource(getResource(), loader.getDataFolder(), getLocation());
        } catch (Exception e) {
            log("File couldn't be loaded.", e, debug);
            return false;
        }

        log("&cFile " + getLocation() + " missing... &7Generating!", debug);
        reload();

        return true;
    }

    public boolean saveDefaults() {
        return saveDefaults(false);
    }

    /**
     * Gets the FileConfiguration object representing the YAML file's configuration.
     * If the configuration is null, it reloads the configuration from the file.
     *
     * @return The FileConfiguration object representing the YAML file's configuration.
     */
    @NotNull
    public FileConfiguration getConfiguration() {
        return configuration == null ? reload() : configuration;
    }

    /**
     * Saves the YAML file to disk.
     *
     * @return True if the file was successfully saved, false otherwise.
     */
    public boolean save(boolean debug) {
        String msg = "&7The &e" + location + "&7 file ";

        try {
            getConfiguration().save(getFile());

            log(msg + "has been&a saved&7.", debug);
            return true;
        }
        catch (Exception e) {
            log(msg + "&ccouldn't be saved&7.", e, debug);
            return false;
        }
    }

    public boolean save() {
        return save(false);
    }

    /**
     * Updates the YAML file from the plugin's resources.
     *
     * @return True if the file was successfully updated, false otherwise.
     */
    public boolean update(boolean debug) {
        String msg = "&7The &e" + getLocation() + "&7 file ";
        if (!isUpdatable()) return false;

        try {
            if (updater == null) loadUpdaterToData(debug);
            updater.update();

            if (ServerInfoUtils.SERVER_VERSION < 13)
                updater.update();

            log(msg + "has been&a updated&7.", debug);
            return true;
        }
        catch (Exception e) {
            log(msg + "&ccouldn't be updated&7.", e, debug);
            return false;
        }
    }

    public boolean update() {
        return update(false);
    }

    /**
     * Returns a string representation of the YAMLFile object.
     *
     * @return A string representation of the YAMLFile object.
     */
    @Override
    public String toString() {
        return "YAMLFile{folder='" + getFolder() + "', name='" + getName() + "'}";
    }

    /**
     * Computes the hash code for the YAMLFile object based on its name, folder, location, file, and resource.
     *
     * @return The hash code value for the YAMLFile object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getFolder(), getLocation(), getFile());
    }

    /**
     * Checks if this YAMLFile object is equal to the specified folder and name.
     *
     * @param folder The folder to compare.
     * @param name   The name to compare.
     * @return True if the folder and name match, false otherwise.
     */
    public boolean equals(String folder, String name) {
        return Objects.equals(this.getFolder(), folder) && Objects.equals(this.getName(), name);
    }

    /**
     * Checks if this YAMLFile object is equal to another object.
     *
     * @param o The object to compare.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (this == o) return true;
        if (getClass() != o.getClass()) return false;

        YAMLFile f = (YAMLFile) o;
        return equals(f.getFolder(), f.getName());
    }
}