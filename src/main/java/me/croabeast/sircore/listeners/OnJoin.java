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
        this.main = main;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (main.hasLogin && main.getConfig().getBoolean("options.login.send-after")) return;

        Player player = event.getPlayer();
        ConfigurationSection section = eventUtils.joinSection(player);
        if (section == null) return;

        eventUtils.getSections(section, player, true);
    }
}
