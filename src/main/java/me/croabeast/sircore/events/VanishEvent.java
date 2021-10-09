package me.croabeast.sircore.events;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.jetbrains.annotations.*;

public class VanishEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean isVanished;

    public VanishEvent(Player player, boolean isVanished) {
        this.player = player;
        this.isVanished = isVanished;
    }

    public Player getPlayer(){
        return player;
    }

    public boolean isVanished() {
        return isVanished;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
