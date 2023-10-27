package me.croabeast.sir.api.event;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Objects;

public abstract class SIRPlayerEvent extends SIRCustomEvent {

    @Getter
    protected final Player player;

    public SIRPlayerEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = Objects.requireNonNull(player);
    }

    public SIRPlayerEvent(Player player) {
        super();
        this.player = Objects.requireNonNull(player);
    }
}
