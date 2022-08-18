package me.croabeast.sirplugin.hook.login;

import fr.xephi.authme.events.*;
import me.croabeast.sirplugin.object.instance.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class AuthMe implements RawViewer {

    public AuthMe() {
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) registerListener();
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.event.LoginEvent(event.getPlayer()));
    }
}
