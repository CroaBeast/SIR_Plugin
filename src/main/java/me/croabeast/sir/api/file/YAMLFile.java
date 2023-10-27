package me.croabeast.sir.api.file;

import lombok.Getter;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sir.api.file.update.YAMLUpdater;
import me.croabeast.sir.api.misc.JavaLoader;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class YAMLFile {

    private final Loader loader;

    /**
     * The name of the file without its extension.
     */
    @Getter
    private final String name;

    /**
     * The folder denomination of the file, can be null.
     */
    @Getter
    private String folder = null;

    /**
     * The location of the file with its extension.
     */
    @Getter
    private final String location;

    /**
     * The raw file that is inside the specified location.
     */
    @Getter @NotNull
    private final File file;

    private InputStream resource = null;

    private FileConfiguration fc = null;

    @Getter
    private boolean updatable = true;

    public <T> YAMLFile(T loader, String folder, String name) throws IOException {
        this.loader = new Loader(loader);
        this.name = StringUtils.isBlank(name) ? ("file-" + hashCode()) : name;

        String location = name + ".yml";

        if (StringUtils.isNotBlank(folder)) {
            this.folder = folder;

            File file = new File(this.loader.getDataFolder(), folder);
            if (!file.exists()) file.mkdirs();

            location = folder + File.separator + location;
        }

        this.location = location;
        this.file = new File(this.loader.getDataFolder(), location);

        try {
            setResource(location);
        } catch (Exception ignored) {}
    }

    public YAMLFile setResource(InputStream resource) throws NullPointerException {
        this.resource = Objects.requireNonNull(resource, "There is no resource in plugin's jar file");
        return this;
    }

    public YAMLFile setResource(String resourcePath) throws NullPointerException {
        if (StringUtils.isBlank(resourcePath))
            throw new NullPointerException("The resource path is blank or empty");

        this.resource = loader.getResource(resourcePath.replace('\\', '/'));
        Objects.requireNonNull(resource, "There is no resource in " + resourcePath);

        return this;
    }

    public YAMLFile setUpdatable(boolean isUpdatable) {
        this.updatable = isUpdatable;
        return this;
    }

    /**
     * Reloads the file to update new edited values.
     *
     * @return the loaded configuration
     */
    @NotNull
    public FileConfiguration reload() {
        return fc = YamlConfiguration.loadConfiguration(getFile());
    }

    /**
     * Similar to {@link JavaPlugin#saveDefaultConfig()}. It will initialize the file.
     *
     * @return true if the file was saved as default or
     *              that file already exists, false otherwise
     */
    public boolean saveDefaults() {
        if (file.exists()) return true;

        try {
            LogUtils.doLog("&cFile " + location + " missing... &7Generating!");
            try {
                JavaLoader.saveResourceFrom(
                        resource, loader.getDataFolder(), location, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            reload();
            return true;
        }
        catch (Exception e) {
            String msg = e.getLocalizedMessage();

            LogUtils.doLog("&cError generating file " + location + ": " + msg);
            return false;
        }
    }

    /**
     * Returns the {@link FileConfiguration} instance of the .yml file.
     *
     * @return the requested configuration
     */
    @NotNull
    public FileConfiguration get() {
        return fc == null ? reload() : fc;
    }

    /**
     * Saves the file to update new set values. It will delete all the file's comments.
     *
     * @param log if you want to show output when the file is saved
     * @return true if the file was saved, false otherwise
     */
    public boolean save(boolean log) {
        String msg = "&7The &e" + location + "&7 file ";

        try {
            get().save(file);

            if (log)
                LogUtils.doLog(msg + "has been&a saved&7.");
            return true;
        }
        catch (Exception e) {
            if (log) {
                LogUtils.doLog(msg + "&ccouldn't be saved&7.");
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
    public boolean save() {
        return save(true);
    }

    /**
     * Tries the file to change all the old values from older versions to newer versions.
     *
     * @param log if you want to show output when the file is saved
     * @return true if the file was updated, false otherwise
     */
    public boolean update(boolean log) {
        String msg = "&7The &e" + location + "&7 file ";
        if (!isUpdatable()) return false;

        try {
            YAMLUpdater.updateFrom(resource, file);
            if (LibUtils.getMainVersion() < 13) YAMLUpdater.updateFrom(resource, file);

            if (log) LogUtils.doLog(msg + "has been&a updated&7.");
            return true;
        }
        catch (Exception e) {
            if (log) {
                LogUtils.doLog(msg + "&ccouldn't be updated&7.");
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Tries the file to change all the old values from older versions to newer versions.
     *
     * @return true if the file was updated, false otherwise
     */
    public boolean update() {
        return update(true);
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

    public <T> T setValue(String path, T value) {
        try {
            get().set(path, value);
            return save(false) ? getValue(path, value) : null;
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

        YAMLFile f = (YAMLFile) o;
        return equals(f.folder, f.name);
    }

    private static class Loader {

        private final Class<?> clazz;
        private final Object loader;

        private <T> Loader(T loader) throws IOException {
            if (loader instanceof Plugin || loader instanceof JavaLoader) {
                this.loader = loader;
                this.clazz = loader.getClass();
                return;
            }

            throw new IOException("Loader object is not valid.");
        }

        String getName() {
            try {
                return (String) clazz.getMethod("getName").invoke(loader);
            } catch (Exception e) {
                return null;
            }
        }

        File getDataFolder() {
            try {
                return (File) clazz.getMethod("getDataFolder").invoke(loader);
            } catch (Exception e) {
                return null;
            }
        }

        InputStream getResource(String name) {
            try {
                return (InputStream) clazz.getMethod("getResource", String.class).invoke(loader, name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        void saveResource(String resourcePath, boolean replace) {
            try {
                loader.getClass()
                        .getMethod("saveResource", String.class, boolean.class)
                        .invoke(loader, resourcePath, replace);
            }
            catch (Exception ignored) {}
        }
    }
}
