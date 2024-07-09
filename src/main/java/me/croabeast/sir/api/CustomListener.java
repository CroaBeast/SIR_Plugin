package me.croabeast.sir.api;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * A custom interface that extends the Listener interface from Bukkit API.
 * This interface provides methods to register and unregister listeners for events.
 */
public interface CustomListener extends Listener {

    boolean isRegistered();

    void setRegistered(boolean registered);

    /**
     * Registers this listener for the given plugin.
     *
     * @param plugin The plugin to register this listener for.
     *
     * @return true if was registered, false otherwise.
     * @throws NullPointerException if the plugin is null.
     */
    default boolean register(Plugin plugin) {
        if (isRegistered()) return false;

        Bukkit.getPluginManager().registerEvents(this, Objects.requireNonNull(plugin));
        setRegistered(true);

        return true;
    }

    /**
     * Unregisters this listener from all registered events.
     */
    default boolean unregister() {
        if (!isRegistered())
            return false;

        HandlerList.unregisterAll(this);
        setRegistered(false);
        return true;
    }
}
