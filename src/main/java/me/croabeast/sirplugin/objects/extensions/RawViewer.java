package me.croabeast.sirplugin.objects.extensions;

import me.croabeast.sirplugin.SIRPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * The interface that registers a bukkit listener.
 * <p>In practice is not useful, but seems more organized.
 */
public interface RawViewer extends Listener {

    /**
     * Registers the bukkit listener.
     */
    default void registerListener() {
        Bukkit.getPluginManager().registerEvents(this, SIRPlugin.getInstance());
    }
}
