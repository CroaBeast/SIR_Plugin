package me.croabeast.sir.api.event.hook;

import me.croabeast.sir.api.event.SIRPlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SIRLoginEvent extends SIRPlayerEvent {

    private static final HandlerList list = new HandlerList();

    public SIRLoginEvent(Player player, boolean isAsync) {
        super(player, isAsync);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return list;
    }
}
