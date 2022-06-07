package me.croabeast.sirplugin.objects.files;

import me.croabeast.sirplugin.*;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.util.Objects;

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
    public YMLFile initialSource() {
        String name = name().toLowerCase().replace("_", "-");
        return SIRPlugin.getInstance().getFiles().getObject(name);
    }

    @NotNull
    public YMLFile source() {
        return Objects.requireNonNull(initialSource());
    }

    @NotNull
    public FileConfiguration get() {
        return source().getFile();
    }
}
