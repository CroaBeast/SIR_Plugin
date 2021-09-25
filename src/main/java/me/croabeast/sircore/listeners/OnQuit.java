package me.croabeast.sircore.listeners;

import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnQuit implements Listener {

    private final MainClass main;
    private final EventUtils eventUtils;

    public OnQuit(MainClass main) {
        this.main = main; main.events++;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);
        if (id == null) return;

        eventUtils.doAllEvent(id, player, false, false);
    }
}
