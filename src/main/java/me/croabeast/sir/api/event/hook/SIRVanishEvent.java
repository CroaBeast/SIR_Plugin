package me.croabeast.sir.api.event.hook;

import lombok.Getter;
import me.croabeast.sir.api.event.SIRPlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SIRVanishEvent extends SIRPlayerEvent {

    private static final HandlerList list = new HandlerList();

    @Getter
    private final boolean isVanished;

    public SIRVanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
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
