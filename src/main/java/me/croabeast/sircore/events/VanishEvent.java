package me.croabeast.sircore.events;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

public class VanishEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final boolean isVanished;

    public VanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
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
