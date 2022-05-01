package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.YMLFile;
import org.bukkit.configuration.file.*;
import org.jetbrains.annotations.*;

import java.util.Objects;

public enum Files {
    // Main files.
    CONFIG,
    LANG,
    MODULES,
    // Chat files.
    EMOJIS,
    FILTERS,
    FORMATS,
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
    public YMLFile fromSource() {
        return Objects.requireNonNull(initialSource());
    }

    @Nullable
    public FileConfiguration initialFile() {
        YMLFile file = initialSource();
        return file == null ? null : file.getFile();
    }

    @NotNull
    public FileConfiguration toFile() {
        return Objects.requireNonNull(initialFile());
    }
}
