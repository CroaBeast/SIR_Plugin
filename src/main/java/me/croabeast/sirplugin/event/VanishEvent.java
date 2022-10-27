package me.croabeast.sirplugin.event;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

public class VanishEvent extends SIREvent {

    private final boolean isVanished;

    public VanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
    }

    public boolean isVanished() {
        return isVanished;
    }
}
