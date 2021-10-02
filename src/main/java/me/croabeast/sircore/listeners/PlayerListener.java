package me.croabeast.sircore.listeners;

import me.croabeast.sircore.Application;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public PlayerListener(Application main) {
        this.main = main;
        main.getInitializer().events++;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(""); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);

        boolean doSpawn = main.getConfig().getBoolean("login.spawn-before");
        boolean sendAfter = main.getConfig().getBoolean("login.send-after");
        boolean vanish = eventUtils.isVanished(player, true);
        boolean silent = main.getConfig().getBoolean("vanish.silent");

        if (main.getInitializer().hasLogin && sendAfter) {
            if (doSpawn) eventUtils.spawn(id, player);
            return;
        }

        if (vanish && silent) return;

        eventUtils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(""); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);
        if (id == null) return;

        boolean vanish = eventUtils.isVanished(player, false);
        if (vanish && main.getConfig().getBoolean("vanish.silent")) return;

        eventUtils.runEvent(id, player, false, false, false);
    }
}
