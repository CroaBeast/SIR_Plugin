package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.hooks.loginhook.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class LoginListener implements Listener {

    private final Application main;

    private final TextUtils text;
    private final PermUtils perms;
    private final EventUtils utils;

    public LoginListener(Application main) {
        this.main = main;

        this.text = main.getTextUtils();
        this.perms = main.getPermUtils();
        this.utils = main.getEventUtils();

        new AuthMe(main);
        new UserLogin(main);
        main.registerListener(this, false);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        if (!text.getOption(2, "enabled")) return;
        if (perms.isVanished(player, true) && text.getOption(3, "silent")) return;

        ConfigurationSection id = utils.lastSection(player, true);
        if (id == null) {
            main.getRecorder().doRecord(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e messages.yml &7file."
            );
            return;
        }

        utils.getLoggedPlayers().add(player);
        utils.runEvent(id, player, true, !text.getOption(2, "enabled"), true);
    }
}
