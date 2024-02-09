package me.croabeast.sir.api.misc;

import me.croabeast.sir.plugin.SIRPlugin;
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

    /**
     * Registers this listener for the given plugin.
     *
     * @param plugin The plugin to register this listener for.
     *
     * @return this listener instance.
     * @throws NullPointerException if the plugin is null.
     */
    default void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, Objects.requireNonNull(plugin));
    }

    /**
     * Registers this listener for the SIR plugin.
     * This method requires access permission from the SIR plugin to operate.
     *
     * @return this listener instance.
     */
    default void registerOnSIR() {
        register(SIRPlugin.getInstance());
    }

    /**
     * Unregisters this listener from all registered events.
     */
    default void unregister() {
        HandlerList.unregisterAll(this);
    }
}
