package me.croabeast.sir.api.misc;

import me.croabeast.sir.plugin.SIRPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface CustomListener extends Listener {

    default void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    default void register() {
        register(SIRPlugin.getInstance());
    }

    default void unregister() {
        HandlerList.unregisterAll(this);
    }
}
