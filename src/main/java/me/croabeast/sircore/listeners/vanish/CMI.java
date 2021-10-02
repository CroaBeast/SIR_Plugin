package me.croabeast.sircore.listeners.vanish;

import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import me.croabeast.sircore.Application;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CMI implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public CMI(Application main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().hasCMI) return;
        main.getInitializer().events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onUnVanish(CMIPlayerUnVanishEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, "join");

        boolean trigger = main.getConfig().getBoolean("vanish.trigger");
        boolean spawn = main.getConfig().getBoolean("vanish.do-spawn");

        if (!main.getInitializer().hasVanish || !trigger) return;

        eventUtils.runEvent(id, player, true, spawn, false);
    }

    @EventHandler
    private void onVanish(CMIPlayerVanishEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, "quit");

        boolean trigger = main.getConfig().getBoolean("vanish.trigger");
        boolean spawn = main.getConfig().getBoolean("vanish.do-spawn");

        if (!main.getInitializer().hasVanish || !trigger) return;

        eventUtils.runEvent(id, player, false, spawn, false);
    }
}
