package me.croabeast.sirplugin.channel;

import lombok.Getter;
import lombok.var;
import me.croabeast.sirplugin.file.FileCache;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public final class GeneralChannel extends AbstractChannel {

    private static ChatChannel defs = null;

    @Nullable @Getter
    private final ChatChannel subChannel;

    public GeneralChannel(ConfigurationSection section) {
        super(section, getDefaults());

        var l = getSection().getConfigurationSection("local");
        subChannel = l == null ? null : new AbstractChannel(l, this) {

            public boolean isGlobal() {
                return false;
            }

            @Nullable
            public ChatChannel getSubChannel() {
                return null;
            }
        };
    }

    public static ChatChannel loadDefaults() {
        var def = FileCache.MODULES.getSection("chat.default");

        return def == null ? null : (defs = new AbstractChannel(def, null) {
            @Nullable
            public ChatChannel getSubChannel() {
                return null;
            }
        });
    }

    public static ChatChannel getDefaults() {
        if (!FileCache.MODULES.getValue("chat.default.enabled", true)) return null;
        return defs == null ? loadDefaults() : defs;
    }
}
