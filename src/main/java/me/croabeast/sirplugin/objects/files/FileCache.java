package me.croabeast.sirplugin.objects.files;

import me.croabeast.sirplugin.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.util.*;

public enum FileCache {
    // Main files.
    CONFIG,
    LANG,
    MODULES,
    // Chat files.
    EMOJIS,
    FILTERS,
    FORMATS,
    MENTIONS,
    // Data files.
    IGNORE,
    // Messages files.
    ADVANCES,
    ANNOUNCES,
    JOIN_QUIT,
    // Misc files.
    DISCORD,
    MOTD;

    @Nullable
    public YMLFile init() {
        String name = name().toLowerCase().replace("_", "-");
        return SIRPlugin.getInstance().getFiles().getObject(name);
    }

    @NotNull
    public YMLFile source() {
        return Objects.requireNonNull(init());
    }

    @NotNull
    public FileConfiguration get() {
        return source().getFile();
    }

    @SuppressWarnings("unchecked")
    public <T> T isSet(String path, T def) {
        return init() == null ? def : (T) get().get(path, def);
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        return get().getConfigurationSection(path);
    }
}
