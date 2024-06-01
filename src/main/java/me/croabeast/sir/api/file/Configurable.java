package me.croabeast.sir.api.file;

import me.croabeast.lib.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Configurable interface defines methods for interacting with configuration data.
 * Implementations of this interface can be used to read and modify configuration settings,
 * such as those stored in YAML files.
 */
@FunctionalInterface
public interface Configurable {

    /**
     * Gets the underlying file configuration.
     *
     * @return The file configuration instance.
     */
    @NotNull FileConfiguration getConfiguration();

    /**
     * Gets a value from the configuration at the specified path.
     *
     * @param <T>   The type of the value to retrieve.
     * @param path  The path to the value.
     * @param clazz The class of the value to retrieve.
     *
     * @return The value at the specified path, or null if not found or
     *         cannot be cast to the specified type.
     */
    @Nullable
    default <T> T get(String path, Class<T> clazz) {
        try {
            return clazz.cast(getConfiguration().get(path));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a value from the configuration at the specified path, with a default value.
     *
     * @param <T> The type of the value to retrieve.
     * @param path The path to the value.
     * @param def  The default value to return if the path does not exist.
     *
     * @return The value at the specified path, or the default value if not found or cannot
     *         be cast to the specified type.
     */
    @SuppressWarnings("unchecked")
    default <T> T get(String path, T def) {
        return (T) getConfiguration().get(path, def);
    }

    /**
     * Sets a value in the configuration at the specified path.
     *
     * @param <T>   The type of the value to set.
     * @param path  The path to set the value.
     * @param value The value to set.
     */
    default <T> void set(String path, T value) {
        getConfiguration().set(path, value);
    }

    /**
     * Gets a configuration section at the specified path.
     *
     * @param path The path to the configuration section.
     * @return The configuration section, or null if not found.
     */
    @Nullable
    default ConfigurationSection getSection(String path) {
        return StringUtils.isBlank(path) ?
                getConfiguration() :
                getConfiguration().getConfigurationSection(path);
    }

    /**
     * Gets a list of keys under the specified path, optionally including nested keys.
     *
     * @param path The path to the keys.
     * @param deep Whether to include nested keys.
     * @return The list of keys.
     */
    @NotNull
    default List<String> getKeys(String path, boolean deep) {
        ConfigurationSection section = getSection(path);

        return section != null ?
                new ArrayList<>(section.getKeys(deep)) :
                new ArrayList<>();
    }

    /**
     * Gets a list of strings from the configuration at the specified path.
     *
     * @param path The path to the list.
     * @return The list of strings.
     */
    default List<String> toStringList(String path) {
        return TextUtils.toList(getConfiguration(), path);
    }
}