package me.croabeast.sirplugin.event;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

public class LoginEvent extends SIREvent {

    public LoginEvent(Player player) {
        super(player);
    }
}
