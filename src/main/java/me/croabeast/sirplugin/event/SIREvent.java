package me.croabeast.sirplugin.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class SIREvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public SIREvent(boolean isAsync) {
        super(isAsync);
    }

    public SIREvent() {
        super();
    }

    public boolean isCancelled() {
        return this instanceof Cancellable && ((Cancellable) this).isCancelled();
    }

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return !isCancelled();
    }

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
