package me.croabeast.sir.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class SIRCustomEvent extends Event {

    public SIRCustomEvent(boolean isAsync) {
        super(isAsync);
    }

    public SIRCustomEvent() {
        super();
    }

    public boolean isCancelled() {
        return this instanceof Cancellable && ((Cancellable) this).isCancelled();
    }

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return !isCancelled();
    }

    public abstract @NotNull HandlerList getHandlers();
}
