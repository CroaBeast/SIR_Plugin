package me.croabeast.sir.api.file;

import me.croabeast.sir.api.ConfigUnit;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Represents a {@link YAMLFile} object that implements the {@link Configurable} interface,
 * providing methods to interact with the configuration settings.
 */
public class ConfigurableFile extends YAMLFile implements Configurable {

    /**
     * Constructs a new {@code ConfigurableFile} with the specified loader, folder, and name.
     *
     * @param loader The loader object used to retrieve data folder and resources.
     * @param folder The folder where the file is located.
     * @param name   The name of the YAML file.
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
    public Map<Integer, Set<ConfigUnit>> getUnitsByPriority(String path) {
        return getUnitsByPriority(getConfiguration(), path);
    }

    /**
     * Gets configuration units sorted by priority from the specified configuration section.
     *
     * @param main The main configuration section.
     * @param path The path to the configuration units.
     *
     * @return A map of configuration units grouped by priority.
     */
    public static Map<Integer, Set<ConfigUnit>> getUnitsByPriority(ConfigurationSection main, String path) {
        ConfigurationSection section = StringUtils.isNotBlank(path) ?
                Objects.requireNonNull(main).getConfigurationSection(path) :
                main;

        Objects.requireNonNull(section);

        Set<String> sectionKeys = section.getKeys(false);
        if (sectionKeys.isEmpty()) throw new NullPointerException();


        Comparator<Integer> sort = Comparator.reverseOrder();
        Map<Integer, Set<ConfigUnit>> map = new TreeMap<>(sort);

        for (String key : sectionKeys) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");
            int def = perm.matches("(?i)default") ? 0 : 1;

            int priority = id.getInt("priority", def);

            Set<ConfigUnit> m = map.getOrDefault(priority, new LinkedHashSet<>());
            m.add(ConfigUnit.of(id));

            map.put(priority, m);
        }

        return map;
    }
}
