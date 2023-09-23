package me.croabeast.sir.api.event;

import lombok.Getter;
import org.bukkit.entity.Player;

public abstract class SIRPlayerEvent extends SIREvent {

    @Getter
    protected final Player player;

    public SIRPlayerEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
    }

    public SIRPlayerEvent(Player player) {
        super();
        this.player = player;
    }
}