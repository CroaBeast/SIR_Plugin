package me.croabeast.sirplugin.chat;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class LocalChannel extends ChatChannel {

    private final boolean enabled;
    private final String name;

    private final boolean useCmd;
    private final String cmdName;

    private final String chatPrefix;

    private final String world;
    private final int radius;

    public LocalChannel(ConfigurationSection id) {
        super(id);

        enabled = id.getBoolean("enabled");
        name = id.getString("channel-name");

        useCmd = id.getBoolean("command.enabled");
        cmdName = id.getString("command.name");

        chatPrefix = id.getString("chat-prefix");

        world = id.getString("world");
        radius = id.getInt("radius");
    }
}
