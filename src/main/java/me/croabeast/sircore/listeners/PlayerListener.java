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

        if (main.getInitializer().hasLogin && main.choice("after")) {
            if (main.choice("lSpawn")) eventUtils.spawn(id, player);
            return;
        }

        if (eventUtils.isVanished(player, true) && main.choice("silent")) return;

        eventUtils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(""); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);
        if (id == null) return;

        if (eventUtils.isVanished(player, false) && main.choice("silent")) return;

        eventUtils.runEvent(id, player, false, false, false);
    }
}
