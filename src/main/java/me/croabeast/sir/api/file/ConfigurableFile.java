package me.croabeast.sir.api.file;

import me.croabeast.lib.file.Configurable;
import me.croabeast.lib.file.YAMLFile;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Represents a {@link YAMLFile} object that implements the {@link Configurable} interface,
 * providing methods to interact with the configuration settings.
 */
public class ConfigurableFile extends me.croabeast.lib.file.ConfigurableFile {

    /**
     * Constructs a new {@code ConfigurableFile} with the specified loader, folder, and name.
     *
     * @param loader The loader object used to retrieve data folder and resources.
     * @param folder The folder where the file is located.
     * @param name   The name of the YAML file.
     *
     * @throws IOException If an I/O error occurs.
     */
    public <T> ConfigurableFile(T loader, @Nullable String folder, String name) throws IOException {
        super(loader, folder, name);
    }

    /**
     * Constructs a new {@code ConfigurableFile} with the specified loader, and name.
     *
     * @param loader The loader object used to retrieve data folder and resources.
     * @param name   The name of the YAML file.
     *
     * @throws IOException If an I/O error occurs.
     */
    public <T> ConfigurableFile(T loader, String name) throws IOException {
        super(loader, name);
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurableFile setResourcePath(String resourcePath) throws NullPointerException {
        super.setResourcePath(resourcePath);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurableFile setUpdatable(boolean updatable) {
        super.setUpdatable(updatable);
        return this;
    }

    /**
     * Gets configuration units sorted by priority from the specified path in the configuration.
     *
     * @param path The path to the configuration units.
     * @return A map of configuration units grouped by priority.
     */
    public Map<Integer, Set<ConfigurationSection>> getUnitsByPriority(String path) {
        return getUnitsByPriority(getConfiguration(), path);
    }
}
