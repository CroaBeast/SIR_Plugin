package me.croabeast.sirplugin.object.file;

import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.object.Sender;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
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
    MOTD,
    WEBHOOKS;

    @Nullable
    public YMLFile init() {
        String name = name().toLowerCase(Locale.ENGLISH).replace("_", "-");
        return SIRPlugin.getInstance().getFiles().getObject(name);
    }

    @NotNull
    public YMLFile source() {
        return Objects.requireNonNull(init());
    }

    public List<String> toList(String path) {
        return TextUtils.toList(get(), path);
    }

    @NotNull
    public FileConfiguration get() {
        return source().getFile();
    }

    @SuppressWarnings("unchecked")
    public <T> T value(String path, T def) {
        return init() == null ? def : (T) get().get(path, def);
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        return get().getConfigurationSection(path);
    }

    public Sender send(String path) {
        return Sender.to(get(), path);
    }

    @Nullable
    public ConfigurationSection permSection(Player player, String path) {
        ConfigurationSection maxSection = null, id = get();
        String maxPerm = null, defKey = null;

        if (player == null) return null;

        if (StringUtils.isNotBlank(path)) id = id.getConfigurationSection(path);
        if (id == null) return null;

        Set<String> keys = id.getKeys(false);
        if (keys.isEmpty()) return null;

        int highestPriority = 0;
        boolean notDef = true;

        for (String k : keys) {
            ConfigurationSection i = id.getConfigurationSection(k);
            if (i == null) continue;

            String perm = i.getString("permission", "DEFAULT");

            if (perm.matches("(?i)DEFAULT") && notDef) {
                defKey = k;
                notDef = false;
                continue;
            }

            int p = i.getInt("priority", perm.matches("(?i)DEFAULT") ? 0 : 1);

            if (PlayerUtils.hasPerm(player, perm, false) && p > highestPriority) {
                maxSection = i;
                maxPerm = perm;
                highestPriority = p;
            }
        }

        if (maxPerm != null && PlayerUtils.hasPerm(player, maxPerm))
            return maxSection;

        return defKey == null ? null : id.getConfigurationSection(defKey);
    }
}
