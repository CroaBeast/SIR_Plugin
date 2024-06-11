package me.croabeast.sir.api;

import lombok.SneakyThrows;
import me.croabeast.sir.plugin.SIRPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
     * @return this listener instance.
     * @throws NullPointerException if the plugin is null.
     */
    default void register(Plugin plugin) {
        if (isRegistered()) return;

        Bukkit.getPluginManager().registerEvents(this, Objects.requireNonNull(plugin));
        setRegistered(true);
    }

    /**
     * Registers this listener for the SIR plugin.
     * This method requires access permission from the SIR plugin to operate.
     *
     * @return this listener instance.
     */
    @SneakyThrows
    default void registerOnSIR() {
        Plugin plugin = null, sir = SIRPlugin.getInstance();
        try {
            plugin = JavaPlugin.getProvidingPlugin(getClass());
        } catch (Exception ignored) {}

        if (!Objects.equals(plugin, sir))
            throw new IllegalAccessException();

        register(sir);
    }

    /**
     * Unregisters this listener from all registered events.
     */
    default void unregister() {
        if (!isRegistered()) return;

        HandlerList.unregisterAll(this);
        setRegistered(false);
    }
}
