package me.croabeast.sircore.events;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.jetbrains.annotations.*;

public class LoginEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public LoginEvent(Player player){
        this.player = player;
    }

    public Player getPlayer(){
        return player;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
