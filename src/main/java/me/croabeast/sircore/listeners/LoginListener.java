package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.listeners.login.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class LoginListener implements Listener {

    private final TextUtils text;
    private final EventUtils utils;

    public LoginListener(Application main) {
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        if (!main.getInitializer().HAS_LOGIN) return;
        new AuthMe(main);
        new UserLogin(main);
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, true);

        if (!text.getOption(2, "enabled")) return;
        if (utils.isVanished(player, true) && text.getOption(3, "silent")) return;

        utils.LOGGED_PLAYERS.add(player);
        utils.runEvent(id, player, true, !text.getOption(2, "enabled"), true);
    }
}
