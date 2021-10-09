package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utils.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

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
        event.setJoinMessage(null); //Message initializer

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
        event.setQuitMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);

        if (eventUtils.isVanished(player, false) && main.choice("silent")) return;
        if (main.getInitializer().hasLogin) {
            if (!eventUtils.loggedPlayers.contains(player)) return;
            eventUtils.loggedPlayers.remove(player);
        }

        eventUtils.runEvent(id, player, false, false, false);
    }
}
