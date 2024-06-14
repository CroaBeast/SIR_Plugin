package me.croabeast.sir.plugin.logger;

import org.bukkit.plugin.Plugin;

public interface DelayLogger {

    DelayLogger add(boolean usePrefix, String line);

    DelayLogger add(boolean usePrefix, String... lines);

    DelayLogger addFirst(boolean usePrefix, String line);

    DelayLogger addFirst(boolean usePrefix, String... lines);

    DelayLogger addLast(boolean usePrefix, String line);

    DelayLogger addLast(boolean usePrefix, String... lines);

    String[] getLoadedLines();

    Plugin getPlugin();

    void setPlugin(Plugin plugin);

    void clear();

    boolean isEmpty();

    void sendLines();

    static DelayLogger simplified(Plugin plugin) {
        return new SimpleLogger(plugin);
    }

    static DelayLogger simplified() {
        return new SimpleLogger(null);
    }

    static DelayLogger queued(Plugin plugin) {
        return new DequeLogger(plugin);
    }

    static DelayLogger queued() {
        return new DequeLogger(null);
    }
}
