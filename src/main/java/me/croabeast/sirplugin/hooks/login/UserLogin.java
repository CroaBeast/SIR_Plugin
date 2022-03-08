package me.croabeast.sirplugin.hooks.login;

import com.elchologamer.userlogin.api.event.*;
import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class UserLogin implements Listener {

    public UserLogin() {
        if (Bukkit.getPluginManager().isPluginEnabled("UserLogin"))
            SIRPlugin.registerListener(this);
    }

    @EventHandler
    private void onLogin(AuthenticationEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.events.LoginEvent(event.getPlayer()));
    }
}
