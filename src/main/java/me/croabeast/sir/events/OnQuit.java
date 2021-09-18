package me.croabeast.sir.events;

import me.croabeast.sir.SIR;
import me.croabeast.sir.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnQuit implements Listener {

    private final SIR main;
    private final EventUtils eventUtils;

    public OnQuit(SIR main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection section = main.getMessages().getConfigurationSection("quit");
        if (section == null) return;

        eventUtils.addPerms(section);
        eventUtils.checkSections(section, player, false);
    }
}
