package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.listeners.login.*;
import me.croabeast.sircore.utils.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class LoginListener implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public LoginListener(Application main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().hasLogin) return;
        new AuthMe(main);
        new UserLogin(main);
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);

        if (!main.choice("after")) return;
        if (eventUtils.isVanished(player, true) && main.choice("silent")) return;

        eventUtils.loggedPlayers.add(player);
        eventUtils.runEvent(id, player, true, !main.choice("login"), true);
    }
}
