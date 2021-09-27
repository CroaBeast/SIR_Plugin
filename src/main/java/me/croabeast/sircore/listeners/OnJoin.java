package me.croabeast.sircore.listeners;

import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoin implements Listener {

    private final MainClass main;
    private final EventUtils eventUtils;

    public OnJoin(MainClass main) {
        this.main = main; main.events++;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);
        boolean doSpawn = main.getConfig().getBoolean("login.spawn-before");

        if (main.hasLogin && main.getConfig().getBoolean("login.send-after")) {
            if (doSpawn) eventUtils.spawn(id, player); return;
        }

        boolean vanish = eventUtils.isVanished(player, true);
        if (vanish && main.getConfig().getBoolean("vanish.silent")) return;

        eventUtils.runEvent(id, player, true, true, false);
    }
}
