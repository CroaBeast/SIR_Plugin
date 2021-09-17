package me.croabeast.sir.events;

import me.croabeast.sir.SIR;
import me.croabeast.sir.utils.LangUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnQuit implements Listener {

    private final SIR main;
    private final LangUtils langUtils;

    public OnQuit(SIR main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection section = main.getMessages().getConfigurationSection("quit");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;
            String perm = id.getString("permission");

            if (perm != null && !player.hasPermission(perm)) continue;

            langUtils.eventSend(player, id + ".send");
        }
    }
}
