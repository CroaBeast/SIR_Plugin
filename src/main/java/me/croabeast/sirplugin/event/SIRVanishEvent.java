package me.croabeast.sirplugin.event;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SIRVanishEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final boolean isVanished;

    public SIRVanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
    }

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return !(this instanceof Cancellable) ||
                !((Cancellable) this).isCancelled();
    }

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
