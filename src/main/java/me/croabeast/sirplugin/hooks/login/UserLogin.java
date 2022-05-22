package me.croabeast.sirplugin.hooks.login;

import com.elchologamer.userlogin.api.event.*;
import me.croabeast.sirplugin.objects.extensions.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class UserLogin implements RawViewer {

    public UserLogin() {
        if (Bukkit.getPluginManager().isPluginEnabled("UserLogin")) registerListener();
    }

    @EventHandler
    private void onLogin(AuthenticationEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.events.LoginEvent(event.getPlayer()));
    }
}
