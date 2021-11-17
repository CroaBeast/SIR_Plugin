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
    private final EventUtils utils;

    public PlayerListener(Application main) {
        this.main = main;
        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
        init.LISTENERS++;
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setJoinMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, true);
        main.getDoUpdate().initUpdater(null);

        if (init.HAS_LOGIN && text.getOption(2, "enabled")) {
            if (text.getOption(2, "spawn-before")) utils.goSpawn(id, player);
            return;
        }
        if (utils.isVanished(player, true) &&
                text.getOption(3, "silent")) return;

        utils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setQuitMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, false);

        if (utils.isVanished(player, false) &&
                text.getOption(3, "silent")) return;
        if (init.HAS_LOGIN) {
            if (!utils.LOGGED_PLAYERS.contains(player)) return;
            utils.LOGGED_PLAYERS.remove(player);
        }

        utils.runEvent(id, player, false, false, false);
    }
}
