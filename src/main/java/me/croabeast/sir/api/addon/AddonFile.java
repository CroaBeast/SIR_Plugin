package me.croabeast.sir.api.addon;

import lombok.Getter;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.file.Configurable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Represents a file containing information about a SIR addon.
 */
@Getter
public final class AddonFile implements Configurable {

    @Getter
    private final FileConfiguration configuration;

    /**
     * The main class of the addon.
     */
    @NotNull
    private final String main;
    /**
     * The name of the addon.
     */
    @NotNull
    private final String name;
    /**
     * The description of the addon (nullable).
     */
    @Nullable
    private final String description;
    /**
     * The version of the addon.
     */
    @NotNull
    private final String version;
    /**
     * The list of authors of the addon.
     */
    @NotNull
    private final List<String> authors;

    public AddonFile(JarFile file) throws NoSuchFileException, NoSuchFieldException {
        if (file == null) throw new NoSuchFileException(null);

        final String fileName = "addon.yml";

        JarEntry entry = file.getJarEntry(fileName);
        if (entry == null)
            throw new NoSuchFileException(fileName);

        final InputStream stream;
        try {
            stream = file.getInputStream(entry);
        } catch (Exception e) {
            throw new NoSuchFileException(fileName);
        }

        InputStreamReader reader = new InputStreamReader(stream);
        configuration = YamlConfiguration.loadConfiguration(reader);

        final String m = get("main", String.class);
        try {
            main = Exceptions.validate(StringUtils::isNotBlank, m);
        } catch (Exception e) {
            throw new NoSuchFieldException(e.getLocalizedMessage());
        }

        name = get("name", "SIRAddon-" + Objects.hashCode(main));

        this.version = get("version", "1.0");
        this.description = get("description", String.class);

        List<String> list = TextUtils.toList(configuration, "authors");
        authors = list.isEmpty() ? ArrayUtils.toList("CroaBeast") : list;
    }

    /**
     * Gets the main and primary author of the addon.
     *
     * @return The main author.
     */
    public String getAuthor() {
        return authors.get(0);
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException Since this file can not be edited.
     */
    public <T> void set(String path, T value) {
        throw new UnsupportedOperationException("This file can't be edited");
    }
}
