package me.croabeast.sir.plugin.channel;

import lombok.Getter;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public final class GeneralChannel extends AbstractChannel implements CacheHandler {

    private static ChatChannel defs = null;

    @Nullable @Getter
    private final ChatChannel subChannel;

    public GeneralChannel(ConfigurationSection section) throws IllegalAccessException {
        super(section, getDefaults());

        ConfigurationSection l = getSection().getConfigurationSection("local");
        subChannel = (l == null || isLocal()) ? null : new AbstractChannel(l, this) {

            public boolean isGlobal() {
                return false;
            }

            @Nullable
            public ChatChannel getSubChannel() {
                return null;
            }
        };
    }

    static FileCache config() {
        return FileCache.CHAT_CHANNELS_CACHE.getConfig();
    }

    static ChatChannel loadDefaults() throws IllegalAccessException {
        ConfigurationSection def = config().getSection("default-channel");

        return def == null ? null : (defs = new AbstractChannel(def, null) {

            public boolean isGlobal() {
                return true;
            }

            @Nullable
            public ChatChannel getSubChannel() {
                return null;
            }
        });
    }

    @Priority(level = 1)
    static void loadCache() {
        try {
            loadDefaults();
        } catch (Exception ignored) {}
    }

    public static ChatChannel getDefaults() {
        if (!config().getValue("default-channel.enabled", true))
            return null;

        try {
            return defs == null ? loadDefaults() : defs;
        } catch (Exception e) {
            return null;
        }
    }
}
