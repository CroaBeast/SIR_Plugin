package me.croabeast.sircore.listeners;

import me.croabeast.sircore.SIRPlugin;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final SIRPlugin main;
    private final EventUtils eventUtils;

    public PlayerListener(SIRPlugin main) {
        this.main = main; main.getMainCore().events++;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(""); //Message initializer

        org.bukkit.entity.Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);
        boolean doSpawn = main.getConfig().getBoolean("login.spawn-before");

        if (main.getMainCore().hasLogin &&
                main.getConfig().getBoolean("login.send-after")) {
            if (doSpawn) eventUtils.spawn(id, player); return;
        }

        boolean vanish = eventUtils.isVanished(player, true);
        if (vanish && main.getConfig().getBoolean("vanish.silent")) return;

        eventUtils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(""); //Message initializer

        org.bukkit.entity.Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);
        if (id == null) return;

        boolean vanish = eventUtils.isVanished(player, false);
        if (vanish && main.getConfig().getBoolean("vanish.silent")) return;

        eventUtils.runEvent(id, player, false, false, false);
    }
}
