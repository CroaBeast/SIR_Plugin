package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final Application main;
    private final Initializer init;
    private final TextUtils text;
    private final PermUtils perms;
    private final EventUtils utils;

    public PlayerListener(Application main) {
        this.main = main;
        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.perms = main.getPermUtils();
        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setJoinMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, true);
        main.getDoUpdate().initUpdater(player);

        if (perms.isVanished(player, true) && text.getOption(3, "silent")) return;
        if (init.HAS_LOGIN && text.getOption(2, "enabled")) {
            if (text.getOption(2, "spawn-before")) utils.goSpawn(id, player);
            return;
        }

        utils.runEvent(id, player, true, true, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setQuitMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, false);

        if (perms.isVanished(player, false) && text.getOption(3, "silent")) return;
        if (init.HAS_LOGIN) {
            if (!utils.getLoggedPlayers().contains(player)) return;
            utils.getLoggedPlayers().remove(player);
        }

        utils.runEvent(id, player, false, false, false);
    }
}
