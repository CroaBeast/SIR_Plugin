package me.croabeast.sirplugin.event;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

public class LoginEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public LoginEvent(Player player) {
        super(player);
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
