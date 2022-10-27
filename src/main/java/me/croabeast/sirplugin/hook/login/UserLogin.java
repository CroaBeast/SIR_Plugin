package me.croabeast.sirplugin.hook.login;

import com.elchologamer.userlogin.api.event.*;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sirplugin.object.instance.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class UserLogin implements RawViewer {

    public UserLogin() {
        if (Exceptions.isPluginEnabled("UserLogin")) registerListener();
    }

    @EventHandler
    private void onLogin(AuthenticationEvent event) {
        Bukkit.getPluginManager().callEvent(
                new me.croabeast.sirplugin.event.LoginEvent(event.getPlayer()));
    }
}
