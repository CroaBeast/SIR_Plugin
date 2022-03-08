package me.croabeast.sirplugin.hooks.login;

import fr.xephi.authme.events.*;
import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class AuthMe implements Listener {

    public AuthMe() {
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe"))
            SIRPlugin.registerListener(this);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.events.LoginEvent(event.getPlayer()));
    }
}
