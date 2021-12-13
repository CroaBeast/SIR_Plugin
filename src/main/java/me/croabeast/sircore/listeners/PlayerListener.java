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

    private boolean isSilent() { return text.getOption(3, "silent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, true);

        main.getAmender().initUpdater(player);
        if (!main.getMessages().getBoolean("enabled", true)) return;
        event.setJoinMessage(null); //Message initializer

        if (perms.isVanished(player, true) && isSilent()) return;
        if (init.HAS_LOGIN && text.getOption(2, "enabled")) {
            if (text.getOption(2, "spawn-before")) utils.goSpawn(id, player);
            return;
        }

        utils.runEvent(id, player, true, true, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, false);

        if (!main.getMessages().getBoolean("enabled", true)) return;
        event.setQuitMessage(null); //Message initializer

        if (perms.isVanished(player, false) && isSilent()) return;
        if (init.HAS_LOGIN) {
            if (!utils.getLoggedPlayers().contains(player)) return;
            utils.getLoggedPlayers().remove(player);
        }

        utils.runEvent(id, player, false, false, false);
    }
}
