package me.croabeast.sir.plugin.channel;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.api.file.Configurable;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.file.YAMLData;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

final class ChatChannelImpl extends AbstractChannel implements DataHandler {

    private static ChatChannel defs = null;

    @Nullable @Getter
    private final ChatChannel subChannel;

    ChatChannelImpl(ConfigurationSection section) throws IllegalAccessException {
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

    static Configurable config() {
        return YAMLData.Module.Chat.CHANNELS.from();
    }

    @SneakyThrows
    static ChatChannel loadDefaults() {
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

    @Priority(1)
    static void loadData() {
        try {
            loadDefaults();
        } catch (Exception ignored) {}
    }

    static ChatChannel getDefaults() {
        if (!config().get("default-channel.enabled", true))
            return null;

        try {
            return defs == null ? loadDefaults() : defs;
        } catch (Exception e) {
            return null;
        }
    }
}
