package me.croabeast.sirplugin.hooks.login;

import fr.xephi.authme.events.*;
import me.croabeast.sirplugin.objects.extensions.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class AuthMe implements RawViewer {

    public AuthMe() {
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) registerListener();
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.events.LoginEvent(event.getPlayer()));
    }
}
