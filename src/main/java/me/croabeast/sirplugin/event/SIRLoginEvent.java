package me.croabeast.sirplugin.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SIRLoginEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public SIRLoginEvent(Player player) {
        super(player);
    }

    public boolean isCancelled() {
        return !(this instanceof Cancellable) || !((Cancellable) this).isCancelled();
    }

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return isCancelled();
    }

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
