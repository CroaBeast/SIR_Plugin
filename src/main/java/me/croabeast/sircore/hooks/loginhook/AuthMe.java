package me.croabeast.sircore.hooks.loginhook;

import fr.xephi.authme.events.*;
import me.croabeast.sircore.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class AuthMe implements Listener {

    public AuthMe(Application main) {
        Initializer init = main.getInitializer();
        if (!init.authMe) return;
        main.registerListener(this);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Bukkit.getPluginManager().callEvent(new me.croabeast.sircore.events.LoginEvent(event.getPlayer()));
    }
}