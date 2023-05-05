package me.croabeast.sirplugin.event;

import lombok.Getter;
import org.bukkit.entity.Player;

public class SIRVanishEvent extends SIRPlayerEvent {

    @Getter
    private final boolean isVanished;

    public SIRVanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
    }
}
