package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.listeners.vanish.*;
import me.croabeast.sircore.utils.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class VanishListener implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public VanishListener(Application main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().hasVanish) return;
        new CMI(main);
        new Essentials(main);
        new Vanish(main);
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onVanish(VanishEvent event) {
        Player player = event.getPlayer();
        boolean vanish = event.isVanished();
        ConfigurationSection id = eventUtils.lastSection(player, vanish ? "join" : "quit");

        if (!main.getInitializer().hasVanish || !main.choice("trigger")) return;
        if(main.getInitializer().hasLogin && !eventUtils.loggedPlayers.contains(player))
            eventUtils.loggedPlayers.add(player);

        eventUtils.runEvent(id, player, vanish, main.choice("vanish"), false);
    }
}
