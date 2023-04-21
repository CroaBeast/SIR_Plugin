package me.croabeast.sirplugin.instance;

import me.croabeast.sirplugin.SIRPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * The interface that registers a bukkit listener using {@link SIRPlugin#getInstance()}.
 * <p>In practice is not useful, but seems more organized.
 */
public interface SIRListener extends Listener {

    /**
     * Registers the bukkit listener.
     */
    default void register() {
        Bukkit.getPluginManager().registerEvents(this, SIRPlugin.getInstance());
    }

    /**
     * Unregisters the bukkit listener.
     */
    default void unregister() {
        HandlerList.unregisterAll(this);
    }
}
